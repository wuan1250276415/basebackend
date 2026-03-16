package com.basebackend.database.health.logger;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.alert.AlertNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 慢查询日志记录器
 * 记录执行时间超过阈值的SQL语句
 */
@Slf4j
@Component
public class SlowQueryLogger {

    private static final int MAX_SQL_LOG_LENGTH = 200;
    private static final int MAX_PARAMETER_METADATA_LENGTH = 120;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern SINGLE_QUOTED_LITERAL_PATTERN = Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern DOUBLE_QUOTED_LITERAL_PATTERN = Pattern.compile("\"(?:\"\"|[^\"])*\"");
    private static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile("(?<![\\w$])-?\\d+(?:\\.\\d+)?(?![\\w$])");

    private final DatabaseEnhancedProperties properties;
    private final AlertNotificationService alertService;

    /**
     * 慢查询统计
     */
    private final Map<String, SlowQueryStats> slowQueryStats = new ConcurrentHashMap<>();

    /**
     * 慢查询总数
     */
    private final AtomicLong totalSlowQueries = new AtomicLong(0);

    public SlowQueryLogger(DatabaseEnhancedProperties properties,
                          AlertNotificationService alertService) {
        this.properties = properties;
        this.alertService = alertService;
    }

    /**
     * 记录慢查询
     * 
     * @param sql SQL语句
     * @param executionTime 执行时间（毫秒）
     * @param parameters SQL参数
     */
    public void logSlowQuery(String sql, long executionTime, Object parameters) {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        long threshold = properties.getHealth().getSlowQueryThreshold();
        if (executionTime < threshold) {
            return;
        }

        // 增加慢查询计数
        totalSlowQueries.incrementAndGet();

        String sanitizedSql = simplifySql(sql);
        String parameterMetadata = buildParameterMetadata(parameters);

        // 记录慢查询日志
        log.warn("SLOW QUERY DETECTED - Execution time: {}ms (threshold: {}ms)\nSQL: {}\nParameter metadata: {}",
                executionTime, threshold, sanitizedSql, parameterMetadata);

        // 发送慢查询告警
        alertService.sendSlowQueryAlert(sanitizedSql, executionTime, threshold);

        // 更新统计信息
        updateSlowQueryStats(sanitizedSql, executionTime);
    }

    /**
     * 记录慢查询（简化版本）
     * 
     * @param sql SQL语句
     * @param executionTime 执行时间（毫秒）
     */
    public void logSlowQuery(String sql, long executionTime) {
        logSlowQuery(sql, executionTime, null);
    }

    /**
     * 更新慢查询统计信息
     */
    private void updateSlowQueryStats(String sql, long executionTime) {
        // 简化SQL（移除参数值）用作统计键
        String simplifiedSql = simplifySql(sql);

        slowQueryStats.compute(simplifiedSql, (key, stats) -> {
            if (stats == null) {
                stats = new SlowQueryStats(simplifiedSql);
            }
            stats.recordExecution(executionTime);
            return stats;
        });
    }

    /**
     * 简化SQL语句（移除具体参数值）
     */
    private String simplifySql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }

        // 移除多余空格
        String simplified = WHITESPACE_PATTERN.matcher(sql).replaceAll(" ").trim();

        // 脱敏SQL中的字面量，尽量规避敏感值泄露
        simplified = SINGLE_QUOTED_LITERAL_PATTERN.matcher(simplified).replaceAll("'?'");
        simplified = DOUBLE_QUOTED_LITERAL_PATTERN.matcher(simplified).replaceAll("\"?\"");
        simplified = NUMERIC_LITERAL_PATTERN.matcher(simplified).replaceAll("?");

        // 限制长度
        return limitLength(simplified, MAX_SQL_LOG_LENGTH);
    }

    /**
     * 生成参数安全元信息（禁止输出具体值）
     */
    private String buildParameterMetadata(Object parameters) {
        if (parameters == null) {
            return "none";
        }

        if (parameters instanceof CharSequence sequence) {
            String normalized = WHITESPACE_PATTERN.matcher(sequence).replaceAll(" ").trim();
            if (looksLikeMetadata(normalized)) {
                return limitLength(normalized, MAX_PARAMETER_METADATA_LENGTH);
            }
            return "type=String,length=" + sequence.length();
        }

        if (parameters instanceof Map<?, ?> map) {
            return "type=%s,size=%d".formatted(parameters.getClass().getSimpleName(), map.size());
        }

        if (parameters instanceof Collection<?> collection) {
            return "type=%s,size=%d".formatted(parameters.getClass().getSimpleName(), collection.size());
        }

        if (parameters.getClass().isArray()) {
            Class<?> componentType = parameters.getClass().getComponentType();
            String componentTypeName = componentType == null ? "Object" : componentType.getSimpleName();
            return "type=%s[],size=%d".formatted(componentTypeName, Array.getLength(parameters));
        }

        return "type=" + parameters.getClass().getSimpleName();
    }

    private boolean looksLikeMetadata(String text) {
        return text.startsWith("count=")
                || text.startsWith("parameterCount=")
                || text.startsWith("type=");
    }

    private String limitLength(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 获取慢查询统计信息
     */
    public Map<String, SlowQueryStats> getSlowQueryStats() {
        return new ConcurrentHashMap<>(slowQueryStats);
    }

    /**
     * 获取慢查询总数
     */
    public long getTotalSlowQueries() {
        return totalSlowQueries.get();
    }

    /**
     * 清除慢查询统计
     */
    public void clearStats() {
        slowQueryStats.clear();
        totalSlowQueries.set(0);
        log.info("Slow query statistics cleared");
    }

    /**
     * 检查是否为慢查询
     */
    public boolean isSlowQuery(long executionTime) {
        return executionTime >= properties.getHealth().getSlowQueryThreshold();
    }

    /**
     * 获取慢查询阈值
     */
    public long getSlowQueryThreshold() {
        return properties.getHealth().getSlowQueryThreshold();
    }

    /**
     * 慢查询统计信息
     */
    public static class SlowQueryStats {
        private final String sql;
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private volatile long maxTime = 0;
        private volatile long minTime = Long.MAX_VALUE;
        private volatile LocalDateTime lastExecutionTime;

        public SlowQueryStats(String sql) {
            this.sql = sql;
        }

        public void recordExecution(long executionTime) {
            count.incrementAndGet();
            totalTime.addAndGet(executionTime);
            
            // 更新最大值
            if (executionTime > maxTime) {
                maxTime = executionTime;
            }
            
            // 更新最小值
            if (executionTime < minTime) {
                minTime = executionTime;
            }
            
            lastExecutionTime = LocalDateTime.now();
        }

        public String getSql() {
            return sql;
        }

        public long getCount() {
            return count.get();
        }

        public long getTotalTime() {
            return totalTime.get();
        }

        public long getAvgTime() {
            long c = count.get();
            return c > 0 ? totalTime.get() / c : 0;
        }

        public long getMaxTime() {
            return maxTime;
        }

        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }

        public LocalDateTime getLastExecutionTime() {
            return lastExecutionTime;
        }

        @Override
        public String toString() {
            return String.format("SlowQueryStats{sql='%s', count=%d, avgTime=%dms, maxTime=%dms, minTime=%dms}",
                    sql, getCount(), getAvgTime(), getMaxTime(), getMinTime());
        }
    }
}
