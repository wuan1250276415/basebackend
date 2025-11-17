package com.basebackend.observability.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
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
 * 数据库连接池健康检查器
 * 检查数据库连接、连接池状态、响应时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // 测试数据库连接
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    return Health.down()
                            .withDetail("message", "Database connection is not valid")
                            .build();
                }

                long responseTime = System.currentTimeMillis() - startTime;

                Map<String, Object> details = new HashMap<>();
                details.put("database", connection.getMetaData().getDatabaseProductName());
                details.put("version", connection.getMetaData().getDatabaseProductVersion());
                details.put("responseTime", responseTime + "ms");
                details.put("catalog", connection.getCatalog());

                // 如果是 HikariCP，获取连接池详细信息
                if (dataSource instanceof HikariDataSource hikariDataSource) {
                    HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

                    if (poolMXBean != null) {
                        Map<String, Object> poolDetails = new HashMap<>();
                        poolDetails.put("active", poolMXBean.getActiveConnections());
                        poolDetails.put("idle", poolMXBean.getIdleConnections());
                        poolDetails.put("total", poolMXBean.getTotalConnections());
                        poolDetails.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                        poolDetails.put("maxPoolSize", hikariDataSource.getMaximumPoolSize());
                        poolDetails.put("minIdle", hikariDataSource.getMinimumIdle());

                        details.put("pool", poolDetails);

                        // 检查连接池是否接近上限
                        int activeConnections = poolMXBean.getActiveConnections();
                        int maxPoolSize = hikariDataSource.getMaximumPoolSize();
                        double utilization = (double) activeConnections / maxPoolSize;

                        details.put("poolUtilization", String.format("%.2f%%", utilization * 100));

                        if (utilization > 0.9) {
                            return Health.down()
                                    .withDetails(details)
                                    .withDetail("message", "Connection pool nearly exhausted")
                                    .build();
                        }

                        // 检查是否有线程等待连接
                        if (poolMXBean.getThreadsAwaitingConnection() > 0) {
                            return Health.down()
                                    .withDetails(details)
                                    .withDetail("message", "Threads waiting for database connections")
                                    .build();
                        }
                    }
                }

                // 检查响应时间
                if (responseTime > 5000) {
                    return Health.down()
                            .withDetails(details)
                            .withDetail("message", "Database response time too slow")
                            .build();
                }

                return Health.up()
                        .withDetails(details)
                        .build();

            }

        } catch (SQLException e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .withDetail("sqlState", e.getSQLState())
                    .withDetail("errorCode", e.getErrorCode())
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }
}
