package com.basebackend.database.health.monitor;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.alert.AlertNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接池监控器
 * 监控连接池的使用情况，当使用率超过阈值时触发告警
 */
@Slf4j
@Component
public class ConnectionPoolMonitor {

    private final DataSource dataSource;
    private final DatabaseEnhancedProperties properties;
    private final AlertNotificationService alertService;

    /**
     * 告警节流：记录上次告警时间（毫秒）
     * 防止短时间内重复发送相同告警
     */
    private final ConcurrentHashMap<String, Long> lastAlertTimeMap = new ConcurrentHashMap<>();

    /**
     * 告警最小间隔（秒）
     */
    private static final long ALERT_MIN_INTERVAL_SECONDS = 60;

    public ConnectionPoolMonitor(DataSource dataSource, 
                                DatabaseEnhancedProperties properties,
                                AlertNotificationService alertService) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.alertService = alertService;
    }

    /**
     * 监控连接池状态
     * @return 连接池统计信息
     */
    public Map<String, Object> monitorConnectionPool() {
        Map<String, Object> stats = new HashMap<>();

        if (!properties.getHealth().isEnabled()) {
            stats.put("enabled", false);
            return stats;
        }

        try {
            if (dataSource instanceof DruidDataSource) {
                DruidDataSource druidDs = (DruidDataSource) dataSource;

                int activeCount = druidDs.getActiveCount();
                int maxActive = druidDs.getMaxActive();

                // 除零防护
                if (maxActive <= 0) {
                    log.warn("Invalid maxActive value: {}", maxActive);
                    stats.put("error", "Invalid maxActive value: " + maxActive);
                    stats.put("resolutionStatus", "Please check datasource configuration");
                    stats.put("errorDetails", Map.of(
                        "maxActive", maxActive,
                        "activeCount", activeCount,
                        "suggestion", "Configure maxActive > 0 in datasource properties"
                    ));
                    return stats;
                }

                int poolingCount = druidDs.getPoolingCount();
                double usageRate = (double) activeCount / maxActive * 100;

                stats.put("activeConnections", activeCount);
                stats.put("idleConnections", poolingCount);
                stats.put("maxConnections", maxActive);
                stats.put("usageRate", usageRate);
                stats.put("createCount", druidDs.getCreateCount());
                stats.put("destroyCount", druidDs.getDestroyCount());
                stats.put("connectCount", druidDs.getConnectCount());
                stats.put("closeCount", druidDs.getCloseCount());
                stats.put("waitThreadCount", druidDs.getWaitThreadCount());
                stats.put("notEmptyWaitCount", druidDs.getNotEmptyWaitCount());
                stats.put("type", "druid");

                // 检查是否需要告警
                if (usageRate >= properties.getHealth().getPoolUsageThreshold()) {
                    triggerPoolUsageAlert(usageRate, activeCount, maxActive);
                }
            } else {
                // 增强的降级处理
                String dataSourceType = dataSource.getClass().getName();
                stats.put("type", "unsupported");
                stats.put("dataSourceType", dataSourceType);
                stats.put("message", "Connection pool monitoring only supports Druid DataSource");
                stats.put("status", "monitoring_unavailable");
                stats.put("fallbackActions", Map.of(
                    "1", "Configure DruidDataSource for detailed monitoring",
                    "2", "Use basic datasource health checks",
                    "3", "Implement custom monitoring for " + dataSourceType
                ));
                log.debug("Unsupported datasource type: {}", dataSourceType);
            }
        } catch (Exception e) {
            log.error("Failed to monitor connection pool", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 获取连接池使用率
     */
    public double getPoolUsageRate() {
        if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDs = (DruidDataSource) dataSource;
            int activeCount = druidDs.getActiveCount();
            int maxActive = druidDs.getMaxActive();
            return (double) activeCount / maxActive * 100;
        }
        return 0.0;
    }

    /**
     * 检查连接池是否健康
     */
    public boolean isPoolHealthy() {
        double usageRate = getPoolUsageRate();
        return usageRate < properties.getHealth().getPoolUsageThreshold();
    }

    /**
     * 触发连接池使用率告警（带节流机制）
     */
    private void triggerPoolUsageAlert(double usageRate, int activeCount, int maxActive) {
        String alertKey = "connection_pool_usage";
        long currentTime = System.currentTimeMillis();
        Long lastAlertTime = lastAlertTimeMap.get(alertKey);

        // 检查是否需要发送告警（节流）
        if (lastAlertTime != null && (currentTime - lastAlertTime) < (ALERT_MIN_INTERVAL_SECONDS * 1000)) {
            log.debug("Alert throttled: last alert was {} seconds ago", (currentTime - lastAlertTime) / 1000);
            return;
        }

        // 更新最后告警时间
        lastAlertTimeMap.put(alertKey, currentTime);

        log.warn("Connection pool usage rate is high: {}% (threshold: {}%)", usageRate, properties.getHealth().getPoolUsageThreshold());

        // 发送告警通知
        try {
            alertService.sendConnectionPoolAlert(
                usageRate,
                activeCount,
                maxActive,
                properties.getHealth().getPoolUsageThreshold()
            );
        } catch (Exception e) {
            log.error("Failed to send connection pool alert", e);
            // 告警发送失败时清除时间戳，允许重试
            lastAlertTimeMap.remove(alertKey);
        }
    }

    /**
     * 获取连接池详细统计信息
     */
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();

        if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDs = (DruidDataSource) dataSource;
            
            stats.put("basic", Map.of(
                "activeCount", druidDs.getActiveCount(),
                "poolingCount", druidDs.getPoolingCount(),
                "maxActive", druidDs.getMaxActive(),
                "minIdle", druidDs.getMinIdle(),
                "initialSize", druidDs.getInitialSize()
            ));

            stats.put("operations", Map.of(
                "createCount", druidDs.getCreateCount(),
                "destroyCount", druidDs.getDestroyCount(),
                "connectCount", druidDs.getConnectCount(),
                "closeCount", druidDs.getCloseCount()
            ));

            stats.put("waiting", Map.of(
                "waitThreadCount", druidDs.getWaitThreadCount(),
                "notEmptyWaitCount", druidDs.getNotEmptyWaitCount(),
                "notEmptyWaitMillis", druidDs.getNotEmptyWaitMillis()
            ));

            stats.put("errors", Map.of(
                "connectErrorCount", druidDs.getConnectErrorCount(),
                "errorCount", druidDs.getErrorCount()
            ));
        }

        return stats;
    }

    /**
     * 获取连接池健康状态摘要
     * 提供快速健康检查结果
     *
     * @return 健康状态摘要
     */
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("timestamp", System.currentTimeMillis());
        summary.put("enabled", properties.getHealth().isEnabled());

        if (!properties.getHealth().isEnabled()) {
            summary.put("status", "DISABLED");
            return summary;
        }

        try {
            if (dataSource instanceof DruidDataSource) {
                DruidDataSource druidDs = (DruidDataSource) dataSource;
                int maxActive = druidDs.getMaxActive();

                // 除零防护
                if (maxActive <= 0) {
                    summary.put("status", "ERROR");
                    summary.put("message", "Invalid datasource configuration");
                    summary.put("errorCode", "INVALID_MAX_ACTIVE");
                    return summary;
                }

                int activeCount = druidDs.getActiveCount();
                double usageRate = (double) activeCount / maxActive * 100;
                double threshold = properties.getHealth().getPoolUsageThreshold();

                boolean isHealthy = usageRate < threshold;
                summary.put("status", isHealthy ? "HEALTHY" : "WARNING");
                summary.put("usageRate", usageRate);
                summary.put("threshold", threshold);
                summary.put("activeConnections", activeCount);
                summary.put("maxConnections", maxActive);
                summary.put("healthScore", Math.max(0, 100 - usageRate));

                if (!isHealthy) {
                    summary.put("warning", String.format("Usage rate %.2f%% exceeds threshold %.2f%%", usageRate, threshold));
                }
            } else {
                summary.put("status", "UNKNOWN");
                summary.put("message", "Unsupported datasource type");
                summary.put("dataSourceType", dataSource.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Failed to get health summary", e);
            summary.put("status", "ERROR");
            summary.put("message", "Failed to get health status");
            summary.put("error", e.getMessage());
        }

        return summary;
    }

    /**
     * 监控路由数据源（如果适用）
     * 支持 AbstractRoutingDataSource 的监控
     *
     * @return 路由数据源统计信息
     */
    public Map<String, Object> monitorRoutingDataSource() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "routing_datasource");
        stats.put("timestamp", System.currentTimeMillis());

        try {
            // 使用反射检查是否为 AbstractRoutingDataSource
            if (dataSource.getClass().getSuperclass().getName().contains("AbstractRoutingDataSource")) {
                // 获取resolvedDataSources字段
                Field resolvedField = dataSource.getClass().getSuperclass().getDeclaredField("resolvedDataSources");
                resolvedField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, DataSource> resolvedDataSources = (Map<Object, DataSource>) resolvedField.get(dataSource);

                stats.put("dataSourceCount", resolvedDataSources.size());
                stats.put("dataSourceKeys", resolvedDataSources.keySet());

                // 监控每个数据源
                Map<String, Map<String, Object>> perDataSourceStats = new HashMap<>();
                for (Map.Entry<Object, DataSource> entry : resolvedDataSources.entrySet()) {
                    String key = entry.getKey().toString();
                    DataSource ds = entry.getValue();

                    if (ds instanceof DruidDataSource) {
                        DruidDataSource druidDs = (DruidDataSource) ds;
                        perDataSourceStats.put(key, Map.of(
                            "activeCount", druidDs.getActiveCount(),
                            "poolingCount", druidDs.getPoolingCount(),
                            "maxActive", druidDs.getMaxActive(),
                            "usageRate", druidDs.getMaxActive() > 0 ?
                                (double) druidDs.getActiveCount() / druidDs.getMaxActive() * 100 : 0
                        ));
                    } else {
                        perDataSourceStats.put(key, Map.of(
                            "status", "unsupported",
                            "type", ds.getClass().getName()
                        ));
                    }
                }
                stats.put("perDataSource", perDataSourceStats);
            } else {
                stats.put("status", "not_routing_datasource");
                stats.put("actualType", dataSource.getClass().getName());
            }
        } catch (NoSuchFieldException e) {
            stats.put("status", "reflection_error");
            stats.put("error", "Field 'resolvedDataSources' not found");
            log.debug("DataSource is not AbstractRoutingDataSource or field not accessible", e);
        } catch (Exception e) {
            log.error("Failed to monitor routing datasource", e);
            stats.put("status", "error");
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
