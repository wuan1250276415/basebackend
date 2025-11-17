package com.basebackend.database.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MyBatis 慢查询监控拦截器
 *
 * 功能：
 * 1. 记录所有 SQL 执行时间
 * 2. 输出慢查询日志（包含完整 SQL 和参数）
 * 3. 统计慢查询 TOP 10
 * 4. 集成 Prometheus 监控指标
 * 5. 提供慢查询告警机制
 *
 * @author 浮浮酱
 */
@Slf4j
//@Component
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        ),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}
        ),
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
public class SlowSqlInterceptor implements Interceptor {

    /**
     * 慢查询阈值（毫秒）
     * 默认 1000ms，可通过配置文件调整
     */
    @Value("${mybatis.slow-sql-threshold:1000}")
    private long slowSqlThreshold;

    /**
     * 是否启用慢查询监控
     */
    @Value("${mybatis.slow-sql-monitor.enabled:true}")
    private boolean enabled;

    /**
     * 是否记录所有 SQL（包括非慢查询）
     */
    @Value("${mybatis.slow-sql-monitor.log-all-sql:false}")
    private boolean logAllSql;

    /**
     * Prometheus 指标注册器（可选）
     */
    private final MeterRegistry meterRegistry;

    /**
     * Prometheus 计数器：慢查询总数
     */
    private Counter slowSqlCounter;

    /**
     * Prometheus 计时器：SQL 执行时间分布
     */
    private Timer sqlExecutionTimer;

    /**
     * 慢查询统计 Map
     * Key: SQL 语句模板（去除参数）
     * Value: 统计信息（执行次数、总时间、平均时间等）
     */
    private static final Map<String, SlowSqlStatistics> SLOW_SQL_STATS = new ConcurrentHashMap<>();

    /**
     * 构造函数（支持无 Prometheus 环境）
     */
    public SlowSqlInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initPrometheusMetrics();
    }

    /**
     * 初始化 Prometheus 监控指标
     */
    private void initPrometheusMetrics() {
        if (meterRegistry != null) {
            try {
                slowSqlCounter = Counter.builder("mybatis.slow.sql.count")
                        .description("慢查询 SQL 总数")
                        .tag("component", "database")
                        .register(meterRegistry);

                sqlExecutionTimer = Timer.builder("mybatis.sql.execution.time")
                        .description("SQL 执行时间分布")
                        .tag("component", "database")
                        .register(meterRegistry);

                log.info("Prometheus 慢查询监控指标初始化完成");
            } catch (Exception e) {
                log.warn("Prometheus 指标初始化失败，慢查询监控仍会通过日志记录: {}", e.getMessage());
            }
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!enabled) {
            return invocation.proceed();
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            // 执行 SQL
            result = invocation.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 记录 SQL 执行时间到 Prometheus
            if (sqlExecutionTimer != null) {
                sqlExecutionTimer.record(executionTime, TimeUnit.MILLISECONDS);
            }

            // 处理慢查询或记录所有 SQL
            if (executionTime >= slowSqlThreshold || logAllSql) {
                handleSlowSql(invocation, executionTime, exception);
            }
        }
    }

    /**
     * 处理慢查询
     */
    private void handleSlowSql(Invocation invocation, long executionTime, Throwable exception) {
        try {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            Object parameter = invocation.getArgs()[1];

            // 获取 SQL 信息
            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            Configuration configuration = mappedStatement.getConfiguration();

            // 获取完整 SQL（包含参数）
            String completeSql = getCompleteSql(boundSql, configuration);

            // 获取 SQL 模板（不含参数，用于统计）
            String sqlTemplate = boundSql.getSql().replaceAll("\\s+", " ").trim();

            // 更新慢查询统计
            updateSlowSqlStatistics(sqlTemplate, executionTime);

            // 记录慢查询到 Prometheus
            if (executionTime >= slowSqlThreshold && slowSqlCounter != null) {
                slowSqlCounter.increment();
            }

            // 输出日志
            logSlowSql(
                    mappedStatement.getId(),
                    completeSql,
                    executionTime,
                    exception
            );

        } catch (Exception e) {
            log.error("处理慢查询监控时发生错误", e);
        }
    }

    /**
     * 获取完整 SQL（包含参数值）
     */
    private String getCompleteSql(BoundSql boundSql, Configuration configuration) {
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

        if (parameterMappings.isEmpty() || parameterObject == null) {
            return sql;
        }

        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        // 替换参数
        for (ParameterMapping parameterMapping : parameterMappings) {
            if (parameterMapping.getMode() != org.apache.ibatis.mapping.ParameterMode.OUT) {
                Object value;
                String propertyName = parameterMapping.getProperty();

                if (boundSql.hasAdditionalParameter(propertyName)) {
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    value = metaObject.getValue(propertyName);
                }

                sql = sql.replaceFirst("\\?", getParameterValue(value));
            }
        }

        return sql;
    }

    /**
     * 格式化参数值
     */
    private String getParameterValue(Object obj) {
        if (obj == null) {
            return "NULL";
        }

        if (obj instanceof String) {
            return "'" + obj + "'";
        }

        if (obj instanceof Date) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "'" + formatter.format((Date) obj) + "'";
        }

        return obj.toString();
    }

    /**
     * 更新慢查询统计信息
     */
    private void updateSlowSqlStatistics(String sqlTemplate, long executionTime) {
        SLOW_SQL_STATS.compute(sqlTemplate, (key, stats) -> {
            if (stats == null) {
                stats = new SlowSqlStatistics(sqlTemplate);
            }
            stats.addExecution(executionTime);
            return stats;
        });
    }

    /**
     * 记录慢查询日志
     */
    private void logSlowSql(String statementId, String completeSql, long executionTime, Throwable exception) {
        String logLevel = executionTime >= slowSqlThreshold ? "WARN" : "DEBUG";

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n================== 慢查询检测 ==================\n");
        logMessage.append("Mapper ID: ").append(statementId).append("\n");
        logMessage.append("执行时间: ").append(executionTime).append(" ms\n");
        logMessage.append("阈值: ").append(slowSqlThreshold).append(" ms\n");
        logMessage.append("完整 SQL: ").append(completeSql).append("\n");

        if (exception != null) {
            logMessage.append("异常信息: ").append(exception.getMessage()).append("\n");
        }

        logMessage.append("================================================\n");

        if ("WARN".equals(logLevel)) {
            log.warn(logMessage.toString());
        } else {
            log.debug(logMessage.toString());
        }
    }

    /**
     * 获取慢查询统计 TOP N
     *
     * @param topN 返回前 N 条
     * @return 慢查询统计列表
     */
    public static List<SlowSqlStatistics> getTopSlowSql(int topN) {
        return SLOW_SQL_STATS.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalTime(), a.getTotalTime()))
                .limit(topN)
                .toList();
    }

    /**
     * 清除慢查询统计
     */
    public static void clearStatistics() {
        SLOW_SQL_STATS.clear();
        log.info("慢查询统计已清除");
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 支持通过 Properties 配置
        if (properties.containsKey("slowSqlThreshold")) {
            this.slowSqlThreshold = Long.parseLong(properties.getProperty("slowSqlThreshold"));
        }
        if (properties.containsKey("enabled")) {
            this.enabled = Boolean.parseBoolean(properties.getProperty("enabled"));
        }
    }

    /**
     * 慢查询统计信息
     */
    public static class SlowSqlStatistics {
        private final String sqlTemplate;
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private long maxTime = 0;
        private long minTime = Long.MAX_VALUE;
        private final Date firstExecutionTime;
        private Date lastExecutionTime;

        public SlowSqlStatistics(String sqlTemplate) {
            this.sqlTemplate = sqlTemplate;
            this.firstExecutionTime = new Date();
            this.lastExecutionTime = new Date();
        }

        public synchronized void addExecution(long executionTime) {
            executionCount.incrementAndGet();
            totalTime.addAndGet(executionTime);
            maxTime = Math.max(maxTime, executionTime);
            minTime = Math.min(minTime, executionTime);
            lastExecutionTime = new Date();
        }

        public String getSqlTemplate() {
            return sqlTemplate;
        }

        public long getExecutionCount() {
            return executionCount.get();
        }

        public long getTotalTime() {
            return totalTime.get();
        }

        public long getAverageTime() {
            long count = executionCount.get();
            return count == 0 ? 0 : totalTime.get() / count;
        }

        public long getMaxTime() {
            return maxTime;
        }

        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }

        public Date getFirstExecutionTime() {
            return firstExecutionTime;
        }

        public Date getLastExecutionTime() {
            return lastExecutionTime;
        }

        @Override
        public String toString() {
            return "SlowSqlStatistics{" +
                    "sqlTemplate='" + sqlTemplate.substring(0, Math.min(100, sqlTemplate.length())) + "...'" +
                    ", executionCount=" + executionCount +
                    ", totalTime=" + totalTime + " ms" +
                    ", averageTime=" + getAverageTime() + " ms" +
                    ", maxTime=" + maxTime + " ms" +
                    ", minTime=" + getMinTime() + " ms" +
                    '}';
        }
    }
}
