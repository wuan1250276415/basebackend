package com.basebackend.database.health.scheduler;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.failover.DataSourceFailoverHandler;
import com.basebackend.database.failover.DataSourceRecoveryManager;
import com.basebackend.database.health.alert.AlertNotificationService;
import com.basebackend.database.health.indicator.DataSourceHealthIndicator;
import com.basebackend.database.health.model.DataSourceHealth;
import com.basebackend.database.health.monitor.ConnectionPoolMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时健康检查调度器
 * 定期检查数据源健康状态和连接池使用情况
 * 集成故障转移和恢复机制
 * 
 * Requirements: 6.1, 6.4
 */
@Slf4j
@Component
public class HealthCheckScheduler {

    private final DataSource dataSource;
    private final DatabaseEnhancedProperties properties;
    private final DataSourceHealthIndicator healthIndicator;
    private final ConnectionPoolMonitor poolMonitor;
    private final AlertNotificationService alertService;
    
    /**
     * 故障转移处理器（可选，用于故障转移功能）
     */
    private DataSourceFailoverHandler failoverHandler;
    
    /**
     * 恢复管理器（可选，用于恢复检测功能）
     */
    private DataSourceRecoveryManager recoveryManager;

    /**
     * 存储最近的健康检查结果
     */
    private final Map<String, DataSourceHealth> latestHealthStatus = new ConcurrentHashMap<>();

    /**
     * 存储最近的连接池监控结果
     */
    private final Map<String, Object> latestPoolStats = new ConcurrentHashMap<>();

    public HealthCheckScheduler(DataSource dataSource,
                               DatabaseEnhancedProperties properties,
                               DataSourceHealthIndicator healthIndicator,
                               ConnectionPoolMonitor poolMonitor,
                               AlertNotificationService alertService) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.healthIndicator = healthIndicator;
        this.poolMonitor = poolMonitor;
        this.alertService = alertService;
    }
    
    /**
     * 注入故障转移处理器（可选）
     */
    @Autowired(required = false)
    public void setFailoverHandler(DataSourceFailoverHandler failoverHandler) {
        this.failoverHandler = failoverHandler;
        log.info("Failover handler integrated with health check scheduler");
    }
    
    /**
     * 注入恢复管理器（可选）
     */
    @Autowired(required = false)
    public void setRecoveryManager(DataSourceRecoveryManager recoveryManager) {
        this.recoveryManager = recoveryManager;
        log.info("Recovery manager integrated with health check scheduler");
    }

    /**
     * 定时执行健康检查
     * 使用配置的检查间隔（默认30秒）
     * 
     * Requirement 6.1: 检测主库连接失败并触发重连
     * Requirement 6.4: 检测从库恢复并加回可用列表
     */
    @Scheduled(fixedDelayString = "${database.enhanced.health.check-interval:30}000")
    public void performHealthCheck() {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        try {
            log.debug("Starting scheduled health check");

            // 检查主数据源
            DataSourceHealth health = healthIndicator.checkDataSource(dataSource, "primary");
            latestHealthStatus.put("primary", health);

            // 记录健康状态变化
            logHealthStatusChange(health);

            // 如果数据源不健康，触发告警和故障转移
            if (health.getStatus() != DataSourceHealth.HealthStatus.UP) {
                triggerHealthAlert(health);
                
                // Requirement 6.1: 主库连接失败时触发故障转移
                if (failoverHandler != null && properties.getFailover().isEnabled()) {
                    log.warn("Master database is unhealthy, triggering failover handler");
                    failoverHandler.handleMasterFailure(dataSource);
                }
            }

            log.debug("Health check completed: {}", health.getStatus());
        } catch (Exception e) {
            log.error("Failed to perform scheduled health check", e);
        }
    }
    
    /**
     * 定时执行恢复检测
     * 检查失败的从库是否已恢复
     * 
     * Requirement 6.4: 从库恢复正常时自动将该节点加回可用列表
     */
    @Scheduled(fixedDelayString = "${database.enhanced.health.check-interval:30}000")
    public void performRecoveryCheck() {
        if (!properties.getHealth().isEnabled() || !properties.getFailover().isEnabled()) {
            return;
        }
        
        if (recoveryManager == null) {
            return;
        }

        try {
            log.debug("Starting scheduled recovery check");
            recoveryManager.performRecoveryCheck();
            log.debug("Recovery check completed");
        } catch (Exception e) {
            log.error("Failed to perform scheduled recovery check", e);
        }
    }

    /**
     * 定时监控连接池
     * 使用配置的检查间隔（默认30秒）
     */
    @Scheduled(fixedDelayString = "${database.enhanced.health.check-interval:30}000")
    public void monitorConnectionPool() {
        if (!properties.getHealth().isEnabled()) {
            return;
        }

        try {
            log.debug("Starting connection pool monitoring");

            Map<String, Object> stats = poolMonitor.monitorConnectionPool();
            latestPoolStats.putAll(stats);

            // 记录连接池统计信息
            if (log.isDebugEnabled() && stats.containsKey("usageRate")) {
                log.debug("Connection pool usage: {}%", stats.get("usageRate"));
            }

            log.debug("Connection pool monitoring completed");
        } catch (Exception e) {
            log.error("Failed to monitor connection pool", e);
        }
    }

    /**
     * 获取最新的健康状态
     */
    public Map<String, DataSourceHealth> getLatestHealthStatus() {
        return new ConcurrentHashMap<>(latestHealthStatus);
    }

    /**
     * 获取最新的连接池统计
     */
    public Map<String, Object> getLatestPoolStats() {
        return new ConcurrentHashMap<>(latestPoolStats);
    }

    /**
     * 获取指定数据源的健康状态
     */
    public DataSourceHealth getHealthStatus(String dataSourceName) {
        return latestHealthStatus.get(dataSourceName);
    }

    /**
     * 记录健康状态变化
     */
    private void logHealthStatusChange(DataSourceHealth health) {
        DataSourceHealth previousHealth = latestHealthStatus.get(health.getName());
        
        if (previousHealth != null && previousHealth.getStatus() != health.getStatus()) {
            log.warn("Data source {} health status changed: {} -> {}", 
                    health.getName(), 
                    previousHealth.getStatus(), 
                    health.getStatus());
            
            if (health.getErrorMessage() != null) {
                log.warn("Error message: {}", health.getErrorMessage());
            }
            
            // 发送健康状态变化告警
            alertService.sendHealthStatusChangeAlert(
                health.getName(),
                previousHealth.getStatus().toString(),
                health.getStatus().toString()
            );
        }
    }

    /**
     * 触发健康告警
     */
    private void triggerHealthAlert(DataSourceHealth health) {
        log.error("Data source {} is unhealthy: {}", health.getName(), health.getStatus());
        
        // 发送数据源故障告警
        alertService.sendDataSourceFailureAlert(
            health.getName(),
            health.getErrorMessage() != null ? health.getErrorMessage() : "Connection test failed"
        );
    }

    /**
     * 手动触发健康检查
     */
    public DataSourceHealth triggerManualHealthCheck(String dataSourceName) {
        if ("primary".equals(dataSourceName)) {
            DataSourceHealth health = healthIndicator.checkDataSource(dataSource, "primary");
            latestHealthStatus.put("primary", health);
            return health;
        }
        return null;
    }

    /**
     * 清除健康检查历史
     */
    public void clearHealthHistory() {
        latestHealthStatus.clear();
        latestPoolStats.clear();
        log.info("Health check history cleared");
    }
}
