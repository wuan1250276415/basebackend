//package com.basebackend.database.health;
//
//import com.alibaba.druid.pool.DruidDataSource;
//import com.basebackend.database.config.DatabaseEnhancedProperties;
//import com.basebackend.database.health.indicator.DataSourceHealthIndicator;
//import com.basebackend.database.health.model.DataSourceHealth;
//import com.basebackend.database.health.monitor.ConnectionPoolMonitor;
//import com.basebackend.database.health.scheduler.HealthCheckScheduler;
//import com.basebackend.database.health.service.HealthMonitoringService;
//import com.basebackend.database.health.service.impl.HealthMonitoringServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.actuate.health.Health;
//import org.springframework.boot.actuate.health.Status;
//
//import javax.sql.DataSource;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 健康监控集成测试
// */
//class HealthMonitoringIntegrationTest {
//
//    private DataSource dataSource;
//    private DatabaseEnhancedProperties properties;
//    private DataSourceHealthIndicator healthIndicator;
//    private ConnectionPoolMonitor poolMonitor;
//    private HealthCheckScheduler scheduler;
//    private HealthMonitoringService monitoringService;
//
//    @BeforeEach
//    void setUp() {
//        // 创建测试用的 Druid 数据源
//        DruidDataSource druidDataSource = new DruidDataSource();
//        druidDataSource.setUrl("jdbc:h2:mem:testdb");
//        druidDataSource.setUsername("sa");
//        druidDataSource.setPassword("");
//        druidDataSource.setDriverClassName("org.h2.Driver");
//        druidDataSource.setInitialSize(2);
//        druidDataSource.setMinIdle(2);
//        druidDataSource.setMaxActive(10);
//        druidDataSource.setTestWhileIdle(true);
//        druidDataSource.setValidationQuery("SELECT 1");
//
//        this.dataSource = druidDataSource;
//
//        // 创建配置
//        properties = new DatabaseEnhancedProperties();
//        properties.getHealth().setEnabled(true);
//        properties.getHealth().setCheckInterval(30);
//        properties.getHealth().setSlowQueryThreshold(1000);
//        properties.getHealth().setPoolUsageThreshold(80);
//
//        // 创建组件
//        healthIndicator = new DataSourceHealthIndicator(dataSource, properties);
////        poolMonitor = new ConnectionPoolMonitor(dataSource, properties);
////        scheduler = new HealthCheckScheduler(dataSource, properties, healthIndicator, poolMonitor);
//        monitoringService = new HealthMonitoringServiceImpl(scheduler, poolMonitor);
//    }
//
//    @Test
//    void testDataSourceHealthIndicator() {
//        // 测试健康检查
//        Health health = healthIndicator.health();
//
//        assertNotNull(health);
//        assertEquals(Status.UP, health.getStatus());
//
//        Map<String, Object> details = health.getDetails();
//        assertNotNull(details);
//        assertTrue(details.containsKey("dataSources"));
//    }
//
//    @Test
//    void testCheckDataSource() {
//        // 测试单个数据源检查
//        DataSourceHealth health = healthIndicator.checkDataSource(dataSource, "test");
//
//        assertNotNull(health);
//        assertEquals("test", health.getName());
//        assertTrue(health.isConnected());
//        assertEquals(DataSourceHealth.HealthStatus.UP, health.getStatus());
//        assertTrue(health.getResponseTime() >= 0);
//        assertNotNull(health.getLastCheckTime());
//    }
//
//    @Test
//    void testConnectionPoolMonitor() {
//        // 测试连接池监控
//        Map<String, Object> stats = poolMonitor.monitorConnectionPool();
//
//        assertNotNull(stats);
//        assertTrue(stats.containsKey("activeConnections"));
//        assertTrue(stats.containsKey("maxConnections"));
//        assertTrue(stats.containsKey("usageRate"));
//
//        // 验证数据类型
//        assertTrue(stats.get("activeConnections") instanceof Integer);
//        assertTrue(stats.get("maxConnections") instanceof Integer);
//        assertTrue(stats.get("usageRate") instanceof Double);
//    }
//
//    @Test
//    void testConnectionPoolUsageRate() {
//        // 测试连接池使用率
//        double usageRate = poolMonitor.getPoolUsageRate();
//
//        assertTrue(usageRate >= 0);
//        assertTrue(usageRate <= 100);
//    }
//
//    @Test
//    void testConnectionPoolHealthy() {
//        // 测试连接池健康状态
//        boolean isHealthy = poolMonitor.isPoolHealthy();
//
//        // 初始状态应该是健康的
//        assertTrue(isHealthy);
//    }
//
//    @Test
//    void testDetailedStats() {
//        // 测试详细统计信息
//        Map<String, Object> detailedStats = poolMonitor.getDetailedStats();
//
//        assertNotNull(detailedStats);
//        assertTrue(detailedStats.containsKey("basic"));
//        assertTrue(detailedStats.containsKey("operations"));
//        assertTrue(detailedStats.containsKey("waiting"));
//        assertTrue(detailedStats.containsKey("errors"));
//    }
//
//    @Test
//    void testHealthCheckScheduler() {
//        // 手动触发健康检查
//        scheduler.performHealthCheck();
//
//        // 获取最新健康状态
//        Map<String, DataSourceHealth> healthStatus = scheduler.getLatestHealthStatus();
//        assertNotNull(healthStatus);
//        assertTrue(healthStatus.containsKey("primary"));
//
//        DataSourceHealth primaryHealth = healthStatus.get("primary");
//        assertNotNull(primaryHealth);
//        assertEquals(DataSourceHealth.HealthStatus.UP, primaryHealth.getStatus());
//    }
//
//    @Test
//    void testConnectionPoolMonitoring() {
//        // 手动触发连接池监控
//        scheduler.monitorConnectionPool();
//
//        // 获取最新连接池统计
//        Map<String, Object> poolStats = scheduler.getLatestPoolStats();
//        assertNotNull(poolStats);
//    }
//
//    @Test
//    void testManualHealthCheck() {
//        // 测试手动触发健康检查
//        DataSourceHealth health = scheduler.triggerManualHealthCheck("primary");
//
//        assertNotNull(health);
//        assertEquals("primary", health.getName());
//        assertTrue(health.isConnected());
//    }
//
//    @Test
//    void testHealthMonitoringService() {
//        // 先执行一次健康检查
//        scheduler.performHealthCheck();
//
//        // 测试服务接口
//        Map<String, DataSourceHealth> allStatus = monitoringService.getAllHealthStatus();
//        assertNotNull(allStatus);
//
//        DataSourceHealth primaryHealth = monitoringService.getHealthStatus("primary");
//        assertNotNull(primaryHealth);
//
//        Map<String, Object> poolStats = monitoringService.getConnectionPoolStats();
//        assertNotNull(poolStats);
//
//        boolean isHealthy = monitoringService.isConnectionPoolHealthy();
//        assertTrue(isHealthy);
//
//        double usageRate = monitoringService.getConnectionPoolUsageRate();
//        assertTrue(usageRate >= 0);
//    }
//
//    @Test
//    void testTriggerHealthCheckViaService() {
//        // 通过服务触发健康检查
//        DataSourceHealth health = monitoringService.triggerHealthCheck("primary");
//
//        assertNotNull(health);
//        assertEquals("primary", health.getName());
//        assertTrue(health.isConnected());
//        assertEquals(DataSourceHealth.HealthStatus.UP, health.getStatus());
//    }
//
//    @Test
//    void testDetailedConnectionPoolStats() {
//        // 测试详细连接池统计
//        Map<String, Object> detailedStats = monitoringService.getDetailedConnectionPoolStats();
//
//        assertNotNull(detailedStats);
//        assertTrue(detailedStats.containsKey("basic"));
//        assertTrue(detailedStats.containsKey("operations"));
//    }
//
//    @Test
//    void testHealthStatusPersistence() {
//        // 执行多次健康检查，验证状态持久化
//        scheduler.performHealthCheck();
//        DataSourceHealth health1 = scheduler.getHealthStatus("primary");
//
//        // 等待一小段时间
//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        scheduler.performHealthCheck();
//        DataSourceHealth health2 = scheduler.getHealthStatus("primary");
//
//        assertNotNull(health1);
//        assertNotNull(health2);
//        // 两次检查的时间应该不同
//        assertNotEquals(health1.getLastCheckTime(), health2.getLastCheckTime());
//    }
//
//    @Test
//    void testClearHealthHistory() {
//        // 执行健康检查
//        scheduler.performHealthCheck();
//        assertFalse(scheduler.getLatestHealthStatus().isEmpty());
//
//        // 清除历史
//        scheduler.clearHealthHistory();
//        assertTrue(scheduler.getLatestHealthStatus().isEmpty());
//    }
//}
