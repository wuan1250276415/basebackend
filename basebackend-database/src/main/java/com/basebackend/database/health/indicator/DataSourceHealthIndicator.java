package com.basebackend.database.health.indicator;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.model.DataSourceHealth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源健康检查指示器
 * 实现 Spring Boot Actuator 的 HealthIndicator 接口
 */
@Slf4j
@Component
public class DataSourceHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final DatabaseEnhancedProperties properties;

    public DataSourceHealthIndicator(DataSource dataSource, 
                                    DatabaseEnhancedProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.getHealth().isEnabled()) {
            return Health.up().withDetail("message", "Health monitoring is disabled").build();
        }

        try {
            Map<String, DataSourceHealth> healthMap = checkAllDataSources();
            
            // 判断整体健康状态
            boolean allHealthy = healthMap.values().stream()
                    .allMatch(h -> h.getStatus() == DataSourceHealth.HealthStatus.UP);
            
            boolean anyDown = healthMap.values().stream()
                    .anyMatch(h -> h.getStatus() == DataSourceHealth.HealthStatus.DOWN);
            
            if (allHealthy) {
                return Health.up()
                        .withDetail("dataSources", healthMap)
                        .build();
            } else if (anyDown) {
                return Health.down()
                        .withDetail("dataSources", healthMap)
                        .build();
            } else {
                return Health.status("DEGRADED")
                        .withDetail("dataSources", healthMap)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to check data source health", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    /**
     * 检查所有数据源
     */
    private Map<String, DataSourceHealth> checkAllDataSources() {
        Map<String, DataSourceHealth> healthMap = new HashMap<>();
        
        // 检查主数据源
        DataSourceHealth primaryHealth = checkDataSource(dataSource, "primary");
        healthMap.put("primary", primaryHealth);
        
        return healthMap;
    }

    /**
     * 检查单个数据源
     */
    public DataSourceHealth checkDataSource(DataSource ds, String name) {
        DataSourceHealth.DataSourceHealthBuilder builder = DataSourceHealth.builder()
                .name(name)
                .lastCheckTime(java.time.LocalDateTime.now());

        try {
            // 测试连接并记录响应时间
            long startTime = System.currentTimeMillis();
            boolean connected = testConnection(ds);
            long responseTime = System.currentTimeMillis() - startTime;

            builder.connected(connected)
                   .responseTime(responseTime);

            if (connected) {
                // 获取连接池信息
                if (ds instanceof DruidDataSource) {
                    DruidDataSource druidDs = (DruidDataSource) ds;
                    int activeCount = druidDs.getActiveCount();
                    int maxActive = druidDs.getMaxActive();
                    int idleCount = maxActive - activeCount;
                    double usageRate = (double) activeCount / maxActive * 100;

                    builder.activeConnections(activeCount)
                           .idleConnections(idleCount)
                           .maxConnections(maxActive)
                           .poolUsageRate(usageRate);

                    // 判断健康状态
                    if (usageRate >= properties.getHealth().getPoolUsageThreshold()) {
                        builder.status(DataSourceHealth.HealthStatus.DEGRADED);
                        log.warn("Data source {} connection pool usage is high: {}%", name, usageRate);
                    } else {
                        builder.status(DataSourceHealth.HealthStatus.UP);
                    }
                } else {
                    builder.status(DataSourceHealth.HealthStatus.UP);
                }
            } else {
                builder.status(DataSourceHealth.HealthStatus.DOWN)
                       .errorMessage("Connection test failed");
                log.error("Data source {} connection test failed", name);
            }
        } catch (Exception e) {
            builder.connected(false)
                   .status(DataSourceHealth.HealthStatus.DOWN)
                   .errorMessage(e.getMessage());
            log.error("Failed to check data source {}", name, e);
        }

        return builder.build();
    }

    /**
     * 测试数据源连接
     */
    private boolean testConnection(DataSource ds) {
        try (Connection conn = ds.getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(5);
        } catch (SQLException e) {
            log.error("Connection test failed", e);
            return false;
        }
    }
}
