package com.basebackend.database.health.service;

import com.basebackend.database.health.model.DataSourceHealth;

import java.util.Map;

/**
 * 健康监控服务接口
 */
public interface HealthMonitoringService {

    /**
     * 获取所有数据源的健康状态
     */
    Map<String, DataSourceHealth> getAllHealthStatus();

    /**
     * 获取指定数据源的健康状态
     */
    DataSourceHealth getHealthStatus(String dataSourceName);

    /**
     * 手动触发健康检查
     */
    DataSourceHealth triggerHealthCheck(String dataSourceName);

    /**
     * 获取连接池统计信息
     */
    Map<String, Object> getConnectionPoolStats();

    /**
     * 获取连接池详细统计信息
     */
    Map<String, Object> getDetailedConnectionPoolStats();

    /**
     * 检查连接池是否健康
     */
    boolean isConnectionPoolHealthy();

    /**
     * 获取连接池使用率
     */
    double getConnectionPoolUsageRate();
}
