package com.basebackend.database.statistics.collector;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.statistics.entity.SqlStatistics;
import com.basebackend.database.statistics.mapper.SqlStatisticsMapper;
import com.basebackend.database.statistics.model.SqlExecutionInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * SQL统计收集器
 * 收集SQL执行统计信息并持久化
 */
@Slf4j
public class SqlStatisticsCollector {

    private final SqlStatisticsMapper sqlStatisticsMapper;
    private final DatabaseEnhancedProperties properties;

    // Local cache to reduce database writes
    private final Cache<String, SqlStatistics> statisticsCache;

    public SqlStatisticsCollector(SqlStatisticsMapper sqlStatisticsMapper,
                                 DatabaseEnhancedProperties properties) {
        this.sqlStatisticsMapper = sqlStatisticsMapper;
        this.properties = properties;

        // Initialize cache with 10 minute expiration
        this.statisticsCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Collect SQL execution statistics
     * This method is called by the interceptor for each SQL execution
     */
    @Async
    public void collect(SqlExecutionInfo executionInfo) {
        if (!properties.getSqlStatistics().isEnabled()) {
            return;
        }

        try {
            String cacheKey = buildCacheKey(executionInfo);

            // Get or create statistics from cache
            SqlStatistics statistics = statisticsCache.get(cacheKey, key -> {
                // Try to load from database
                SqlStatistics existing = sqlStatisticsMapper.selectByMd5(executionInfo.getSqlMd5());
                if (existing != null) {
                    return existing;
                }

                // Create new statistics
                SqlStatistics newStats = new SqlStatistics();
                newStats.setSqlMd5(executionInfo.getSqlMd5());
                newStats.setSqlTemplate(executionInfo.getSqlTemplate());
                newStats.setExecuteCount(0L);
                newStats.setTotalTime(0L);
                newStats.setAvgTime(0L);
                newStats.setMaxTime(0L);
                newStats.setMinTime(Long.MAX_VALUE);
                newStats.setFailCount(0L);
                newStats.setDataSourceName(executionInfo.getDataSourceName());
                newStats.setTenantId(executionInfo.getTenantId());
                return newStats;
            });

            // Update statistics
            updateStatistics(statistics, executionInfo);

            // Persist to database
            persistStatistics(statistics);

        } catch (Exception e) {
            log.error("Failed to collect SQL statistics", e);
        }
    }

    /**
     * Update statistics with new execution info
     */
    private void updateStatistics(SqlStatistics statistics, SqlExecutionInfo executionInfo) {
        synchronized (statistics) {
            // Update execution count
            statistics.setExecuteCount(statistics.getExecuteCount() + 1);

            // Update time statistics
            long executionTime = executionInfo.getExecutionTime();
            statistics.setTotalTime(statistics.getTotalTime() + executionTime);
            statistics.setAvgTime(statistics.getTotalTime() / statistics.getExecuteCount());

            // Update max time
            if (executionTime > statistics.getMaxTime()) {
                statistics.setMaxTime(executionTime);
            }

            // Update min time
            if (executionTime < statistics.getMinTime()) {
                statistics.setMinTime(executionTime);
            }

            // Update fail count
            if (!executionInfo.getSuccess()) {
                statistics.setFailCount(statistics.getFailCount() + 1);
            }

            // Update last execute time
            statistics.setLastExecuteTime(executionInfo.getExecuteTime());
        }
    }

    /**
     * Persist statistics to database
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistStatistics(SqlStatistics statistics) {
        try {
            if (statistics.getId() == null) {
                // Insert new record
                sqlStatisticsMapper.insert(statistics);
            } else {
                // Update existing record
                sqlStatisticsMapper.updateById(statistics);
            }
        } catch (Exception e) {
            log.error("Failed to persist SQL statistics", e);
        }
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(SqlExecutionInfo executionInfo) {
        StringBuilder key = new StringBuilder(executionInfo.getSqlMd5());
        if (executionInfo.getDataSourceName() != null) {
            key.append(":").append(executionInfo.getDataSourceName());
        }
        if (executionInfo.getTenantId() != null) {
            key.append(":").append(executionInfo.getTenantId());
        }
        return key.toString();
    }

    /**
     * Flush cache to database
     * This method can be called periodically to ensure data persistence
     */
    public void flushCache() {
        log.info("Flushing SQL statistics cache to database");
        statisticsCache.asMap().values().forEach(this::persistStatistics);
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        log.info("Clearing SQL statistics cache");
        statisticsCache.invalidateAll();
    }
}
