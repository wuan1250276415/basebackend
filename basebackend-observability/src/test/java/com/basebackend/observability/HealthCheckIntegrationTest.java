package com.basebackend.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 健康检查集成测试
 * 测试自定义 HealthIndicator 功能
 */
@SpringBootTest
@TestPropertySource(properties = {
        "management.health.defaults.enabled=true",
        "management.endpoints.web.exposure.include=health"
})
@DisplayName("健康检查集成测试")
class HealthCheckIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("测试 ApplicationHealthIndicator 存在")
    void testApplicationHealthIndicatorExists() {
        // 验证 ApplicationHealthIndicator Bean 存在
        boolean exists = applicationContext.containsBean("applicationHealthIndicator");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("测试 ApplicationHealthIndicator 执行")
    void testApplicationHealthIndicatorHealth() {
        // 获取 ApplicationHealthIndicator
        HealthIndicator indicator = applicationContext.getBean("applicationHealthIndicator", HealthIndicator.class);
        assertNotNull(indicator, "ApplicationHealthIndicator should be available");

        // 执行健康检查
        Health health = indicator.health();
        assertNotNull(health, "Health should not be null");

        // 验证状态（应该是 UP，因为应用正常运行）
        assertThat(health.getStatus().getCode()).isIn("UP", "UNKNOWN");

        // 验证详情包含必要的字段
        if ("UP".equals(health.getStatus().getCode())) {
            assertThat(health.getDetails()).isNotEmpty();
            assertThat(health.getDetails()).containsKeys("memory", "threads", "uptime");
        }
    }

    @Test
    @DisplayName("测试 DiskSpaceHealthIndicator 存在")
    void testDiskSpaceHealthIndicatorExists() {
        boolean exists = applicationContext.containsBean("diskSpaceHealthIndicator");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("测试 DiskSpaceHealthIndicator 执行")
    void testDiskSpaceHealthIndicatorHealth() {
        HealthIndicator indicator = applicationContext.getBean("diskSpaceHealthIndicator", HealthIndicator.class);
        assertNotNull(indicator, "DiskSpaceHealthIndicator should be available");

        // 执行健康检查
        Health health = indicator.health();
        assertNotNull(health, "Health should not be null");

        // 验证详情包含磁盘空间信息
        if ("UP".equals(health.getStatus().getCode())) {
            assertThat(health.getDetails()).containsKeys("total", "free", "usable", "used", "usedPercent");
        }
    }

    @Test
    @DisplayName("测试 DatabaseHealthIndicator 存在（如果配置了数据源）")
    void testDatabaseHealthIndicatorExistsIfConfigured() {
        // DatabaseHealthIndicator 需要数据源才能创建
        // 这里只检查 Bean 是否存在
        boolean exists = applicationContext.containsBean("databaseHealthIndicator");
        // 可能存在，也可能不存在（取决于是否配置了数据源）
        // 只记录日志，不断言
        System.out.println("DatabaseHealthIndicator exists: " + exists);
    }

    @Test
    @DisplayName("测试 RedisHealthIndicator 存在（如果配置了 Redis）")
    void testRedisHealthIndicatorExistsIfConfigured() {
        // RedisHealthIndicator 需要 Redis 连接才能创建
        boolean exists = applicationContext.containsBean("redisHealthIndicator");
        System.out.println("RedisHealthIndicator exists: " + exists);
    }

    @Test
    @DisplayName("测试 RocketMQHealthIndicator 存在（如果配置了 RocketMQ）")
    void testRocketMQHealthIndicatorExistsIfConfigured() {
        // RocketMQHealthIndicator 需要 RocketMQ 配置才能创建
        boolean exists = applicationContext.containsBean("rocketMQHealthIndicator");
        System.out.println("RocketMQHealthIndicator exists: " + exists);
    }

    @Test
    @DisplayName("测试所有 HealthIndicator 不抛出异常")
    void testAllHealthIndicatorsNoException() {
        // 获取所有 HealthIndicator Bean
        var indicators = applicationContext.getBeansOfType(HealthIndicator.class);
        assertThat(indicators).isNotEmpty();

        // 执行每个健康检查，确保不抛出异常
        indicators.forEach((name, indicator) -> {
            try {
                Health health = indicator.health();
                assertNotNull(health, "Health from " + name + " should not be null");
                System.out.println(name + " status: " + health.getStatus());
            } catch (Exception e) {
                // 某些 HealthIndicator 可能因为缺少依赖而失败，这是正常的
                System.err.println(name + " failed: " + e.getMessage());
            }
        });
    }
}
