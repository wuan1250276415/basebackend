package com.basebackend.database.health.logger;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.alert.AlertNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 慢查询日志记录器
 * 记录执行时间超过阈值的SQL语句
 */
@Slf4j
@Component
public class SlowQueryLogger {

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

        // 记录慢查询日志
        log.warn("SLOW QUERY DETECTED - Execution time: {}ms (threshold: {}ms)\nSQL: {}\nParameters: {}",
                executionTime, threshold, sql, parameters);

        // 发送慢查询告警
        alertService.sendSlowQueryAlert(sql, executionTime, threshold);

        // 更新统计信息
        updateSlowQueryStats(sql, executionTime);
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
        if (sql == null) {
            return "";
        }
        // 移除多余空格
        String simplified = sql.replaceAll("\\s+", " ").trim();
        // 限制长度
        if (simplified.length() > 200) {
            simplified = simplified.substring(0, 200) + "...";
        }
        return simplified;
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
