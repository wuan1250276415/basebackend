package com.basebackend.database.health.service.impl;

import com.basebackend.database.health.model.DataSourceHealth;
import com.basebackend.database.health.monitor.ConnectionPoolMonitor;
import com.basebackend.database.health.scheduler.HealthCheckScheduler;
import com.basebackend.database.health.service.HealthMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 健康监控服务实现
 */
@Slf4j
@Service
public class HealthMonitoringServiceImpl implements HealthMonitoringService {

    private final HealthCheckScheduler healthCheckScheduler;
    private final ConnectionPoolMonitor connectionPoolMonitor;

    public HealthMonitoringServiceImpl(HealthCheckScheduler healthCheckScheduler,
                                      ConnectionPoolMonitor connectionPoolMonitor) {
        this.healthCheckScheduler = healthCheckScheduler;
        this.connectionPoolMonitor = connectionPoolMonitor;
    }

    @Override
    public Map<String, DataSourceHealth> getAllHealthStatus() {
        return healthCheckScheduler.getLatestHealthStatus();
    }

    @Override
    public DataSourceHealth getHealthStatus(String dataSourceName) {
        return healthCheckScheduler.getHealthStatus(dataSourceName);
    }

    @Override
    public DataSourceHealth triggerHealthCheck(String dataSourceName) {
        log.info("Manually triggering health check for data source: {}", dataSourceName);
        return healthCheckScheduler.triggerManualHealthCheck(dataSourceName);
    }

    @Override
    public Map<String, Object> getConnectionPoolStats() {
        return healthCheckScheduler.getLatestPoolStats();
    }

    @Override
    public Map<String, Object> getDetailedConnectionPoolStats() {
        return connectionPoolMonitor.getDetailedStats();
    }

    @Override
    public boolean isConnectionPoolHealthy() {
        return connectionPoolMonitor.isPoolHealthy();
    }

    @Override
    public double getConnectionPoolUsageRate() {
        return connectionPoolMonitor.getPoolUsageRate();
    }
}
