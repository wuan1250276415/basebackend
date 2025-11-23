package com.basebackend.database.health.monitor;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.alert.AlertNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

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

                // 检查是否需要告警
                if (usageRate >= properties.getHealth().getPoolUsageThreshold()) {
                    triggerPoolUsageAlert(usageRate, activeCount, maxActive);
                }
            } else {
                stats.put("type", "unknown");
                stats.put("message", "Connection pool monitoring only supports Druid DataSource");
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
     * 触发连接池使用率告警
     */
    private void triggerPoolUsageAlert(double usageRate, int activeCount, int maxActive) {
        log.warn("Connection pool usage rate is high: {}% (threshold: {}%)", usageRate, properties.getHealth().getPoolUsageThreshold());
        
        // 发送告警通知
        alertService.sendConnectionPoolAlert(
            usageRate, 
            activeCount, 
            maxActive, 
            properties.getHealth().getPoolUsageThreshold()
        );
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
}
