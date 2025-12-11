package com.basebackend.generator.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.generator.constant.GeneratorConstants;
import com.basebackend.generator.entity.DatabaseType;
import com.basebackend.generator.entity.GenDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据源工具类
 * 
 * 修复资源管理问题：
 * 1. 使用缓存避免重复创建数据源
 * 2. 提供数据源的正确关闭机制
 * 3. 支持定期清理过期数据源
 */
@Slf4j
public class DataSourceUtils {

    /**
     * 数据源缓存
     * Key: 数据源ID, Value: 缓存条目（包含数据源和最后访问时间）
     */
    private static final Map<Long, DataSourceCacheEntry> DATA_SOURCE_CACHE = new ConcurrentHashMap<>();

    /**
     * 定期清理过期数据源的调度器
     */
    private static final ScheduledExecutorService CLEANUP_SCHEDULER;

    /**
     * 初始化关闭钩子和定期清理任务
     */
    static {
        // 注册JVM关闭钩子，确保所有数据源正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM关闭，正在清理所有缓存的数据源...");
            closeAllDataSources();
        }, "datasource-cleanup-hook"));

        // 启动定期清理过期数据源的任务（每10分钟检查一次）
        CLEANUP_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "datasource-cleanup-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        CLEANUP_SCHEDULER.scheduleAtFixedRate(
                DataSourceUtils::cleanupExpiredDataSources,
                10, 10, TimeUnit.MINUTES);
    }

    /**
     * 获取或创建数据源（带缓存）
     *
     * @param config 数据源配置
     * @return 数据源实例
     */
    public static DataSource getOrCreateDataSource(GenDataSource config) {
        Long id = config.getId();
        if (id == null) {
            // 没有ID的配置，直接创建（用于测试连接等临时场景）
            return createDataSource(config);
        }

        // 从缓存获取或创建
        DataSourceCacheEntry entry = DATA_SOURCE_CACHE.compute(id, (key, existing) -> {
            if (existing != null && existing.isValid()) {
                existing.updateLastAccessTime();
                return existing;
            }
            // 关闭旧的数据源（如果存在）
            if (existing != null) {
                closeDataSourceQuietly(existing.getDataSource());
            }
            // 创建新的数据源
            DataSource newDs = createDataSource(config);
            return new DataSourceCacheEntry(newDs);
        });

        return entry.getDataSource();
    }

    /**
     * 创建数据源（不缓存）
     * 
     * @param config 数据源配置
     * @return 新创建的数据源
     */
    public static DataSource createDataSource(GenDataSource config) {
        DatabaseType dbType = DatabaseType.valueOf(config.getDbType());
        String url = dbType.buildUrl(config.getHost(), config.getPort(), config.getDatabaseName());

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setDriverClassName(dbType.getDriverClass());

        // 连接池配置（使用常量）
        dataSource.setInitialSize(GeneratorConstants.POOL_INITIAL_SIZE);
        dataSource.setMinIdle(GeneratorConstants.POOL_MIN_IDLE);
        dataSource.setMaxActive(GeneratorConstants.POOL_MAX_ACTIVE);
        dataSource.setMaxWait(GeneratorConstants.POOL_MAX_WAIT_MILLIS);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery(GeneratorConstants.POOL_VALIDATION_QUERY);

        // 定期验证连接
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);

        log.debug("创建数据源: {} - {}", config.getName(), url);
        return dataSource;
    }

    /**
     * 测试数据源连接
     *
     * @param dataSource 数据源
     * @return true表示连接成功，false表示连接失败
     */
    public static boolean testConnection(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.error("数据源连接测试失败", e);
            return false;
        }
    }

    /**
     * 从缓存中移除并关闭指定数据源
     *
     * @param datasourceId 数据源ID
     */
    public static void removeFromCache(Long datasourceId) {
        if (datasourceId == null) {
            return;
        }
        DataSourceCacheEntry entry = DATA_SOURCE_CACHE.remove(datasourceId);
        if (entry != null) {
            closeDataSourceQuietly(entry.getDataSource());
            log.info("从缓存移除并关闭数据源: {}", datasourceId);
        }
    }

    /**
     * 关闭数据源
     *
     * @param dataSource 数据源
     */
    public static void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDs = (DruidDataSource) dataSource;
            if (!druidDs.isClosed()) {
                druidDs.close();
                log.debug("数据源已关闭");
            }
        }
    }

    /**
     * 静默关闭数据源（不抛出异常）
     *
     * @param dataSource 数据源
     */
    private static void closeDataSourceQuietly(DataSource dataSource) {
        try {
            closeDataSource(dataSource);
        } catch (Exception e) {
            log.warn("关闭数据源时发生错误", e);
        }
    }

    /**
     * 关闭所有缓存的数据源
     */
    public static void closeAllDataSources() {
        log.info("正在关闭所有缓存的数据源，共 {} 个", DATA_SOURCE_CACHE.size());
        DATA_SOURCE_CACHE.forEach((id, entry) -> {
            try {
                closeDataSource(entry.getDataSource());
                log.debug("关闭数据源: {}", id);
            } catch (Exception e) {
                log.warn("关闭数据源 {} 时发生错误", id, e);
            }
        });
        DATA_SOURCE_CACHE.clear();
    }

    /**
     * 清理过期的数据源
     */
    private static void cleanupExpiredDataSources() {
        log.debug("开始清理过期数据源...");
        int cleanedCount = 0;

        for (Map.Entry<Long, DataSourceCacheEntry> entry : DATA_SOURCE_CACHE.entrySet()) {
            if (entry.getValue().isExpired()) {
                DATA_SOURCE_CACHE.remove(entry.getKey());
                closeDataSourceQuietly(entry.getValue().getDataSource());
                cleanedCount++;
                log.debug("清理过期数据源: {}", entry.getKey());
            }
        }

        if (cleanedCount > 0) {
            log.info("清理了 {} 个过期数据源", cleanedCount);
        }
    }

    /**
     * 获取缓存大小
     *
     * @return 当前缓存的数据源数量
     */
    public static int getCacheSize() {
        return DATA_SOURCE_CACHE.size();
    }

    /**
     * 数据源缓存条目
     */
    private static class DataSourceCacheEntry {
        private final DataSource dataSource;
        private volatile long lastAccessTime;

        public DataSourceCacheEntry(DataSource dataSource) {
            this.dataSource = dataSource;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        /**
         * 检查数据源是否仍然有效
         */
        public boolean isValid() {
            if (dataSource instanceof DruidDataSource) {
                return !((DruidDataSource) dataSource).isClosed();
            }
            return true;
        }

        /**
         * 检查是否过期（超过缓存过期时间未访问）
         */
        public boolean isExpired() {
            long idleTime = System.currentTimeMillis() - lastAccessTime;
            return idleTime > GeneratorConstants.DATASOURCE_CACHE_EXPIRE_SECONDS * 1000;
        }
    }
}
