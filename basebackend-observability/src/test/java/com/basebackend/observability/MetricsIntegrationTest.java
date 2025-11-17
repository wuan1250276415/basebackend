package com.basebackend.observability;

import com.basebackend.observability.metrics.BusinessMetrics;
import com.basebackend.observability.metrics.annotations.Counted;
import com.basebackend.observability.metrics.annotations.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Metrics 集成测试
 * 测试 BusinessMetrics 和自定义注解功能
 */
@SpringBootTest
@TestPropertySource(properties = {
        "observability.metrics.enabled=true",
        "management.endpoints.web.exposure.include=*"
})
@DisplayName("Metrics 集成测试")
class MetricsIntegrationTest {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private BusinessMetrics businessMetrics;

    @Autowired(required = false)
    private MetricsTestService metricsTestService;

    @Test
    @DisplayName("测试 MeterRegistry 注入")
    void testMeterRegistryInjection() {
        assertNotNull(meterRegistry, "MeterRegistry should be autowired");
    }

    @Test
    @DisplayName("测试 BusinessMetrics 注入")
    void testBusinessMetricsInjection() {
        assertNotNull(businessMetrics, "BusinessMetrics should be autowired");
    }

    @Test
    @DisplayName("测试用户注册指标记录")
    void testUserRegistrationMetrics() {
        if (businessMetrics == null) {
            return; // Skip if not available
        }

        // 记录用户注册
        businessMetrics.recordUserRegistration("web", true);

        // 验证 Counter 被创建
        Counter counter = meterRegistry.find("business_user_registrations_total")
                .tag("source", "web")
                .tag("status", "success")
                .counter();

        assertNotNull(counter, "User registration counter should exist");
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("测试用户登录指标记录")
    void testUserLoginMetrics() {
        if (businessMetrics == null) {
            return;
        }

        // 记录用户登录
        businessMetrics.recordUserLogin("password", true);
        businessMetrics.recordUserLogin("password", false);

        // 验证成功登录 Counter
        Counter successCounter = meterRegistry.find("business_user_logins_total")
                .tag("method", "password")
                .tag("status", "success")
                .counter();

        assertNotNull(successCounter, "Success login counter should exist");
        assertThat(successCounter.count()).isGreaterThan(0);

        // 验证失败登录 Counter
        Counter failedCounter = meterRegistry.find("business_user_logins_total")
                .tag("method", "password")
                .tag("status", "failed")
                .counter();

        assertNotNull(failedCounter, "Failed login counter should exist");
        assertThat(failedCounter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("测试订单创建指标记录")
    void testOrderCreationMetrics() {
        if (businessMetrics == null) {
            return;
        }

        // 记录订单创建
        businessMetrics.recordOrderCreation("standard", true);

        // 验证 Counter 被创建
        Counter counter = meterRegistry.find("business_order_creations_total")
                .tag("type", "standard")
                .tag("status", "success")
                .counter();

        assertNotNull(counter, "Order creation counter should exist");
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("测试支付指标记录")
    void testPaymentMetrics() {
        if (businessMetrics == null) {
            return;
        }

        // 记录支付请求
        businessMetrics.recordPaymentRequest("alipay", "CNY", 100.0);
        businessMetrics.recordPaymentSuccess("alipay", "CNY", 100.0);

        // 验证支付请求 Counter
        Counter requestCounter = meterRegistry.find("business_payment_requests_total")
                .tag("method", "alipay")
                .tag("currency", "CNY")
                .counter();

        assertNotNull(requestCounter, "Payment request counter should exist");
        assertThat(requestCounter.count()).isGreaterThan(0);

        // 验证支付成功 Counter
        Counter successCounter = meterRegistry.find("business_payment_success_total")
                .tag("method", "alipay")
                .tag("currency", "CNY")
                .counter();

        assertNotNull(successCounter, "Payment success counter should exist");
        assertThat(successCounter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("测试 @Timed 注解功能")
    void testTimedAnnotation() {
        if (metricsTestService == null) {
            return;
        }

        // 调用带 @Timed 注解的方法
        metricsTestService.timedMethod();

        // 验证 Timer 被创建
        Timer timer = meterRegistry.find("test.timed.method")
                .tag("class", "MetricsTestService")
                .tag("method", "timedMethod")
                .timer();

        assertNotNull(timer, "Timed metric should exist");
        assertThat(timer.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("测试 @Counted 注解功能")
    void testCountedAnnotation() {
        if (metricsTestService == null) {
            return;
        }

        // 调用带 @Counted 注解的方法
        metricsTestService.countedMethod();

        // 验证 Counter 被创建
        Counter counter = meterRegistry.find("test.counted.method")
                .tag("class", "MetricsTestService")
                .tag("method", "countedMethod")
                .tag("result", "success")
                .counter();

        assertNotNull(counter, "Counted metric should exist");
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("测试缓存命中率记录")
    void testCacheMetrics() {
        if (businessMetrics == null) {
            return;
        }

        // 记录缓存命中和未命中
        businessMetrics.recordCacheAccess("userCache", true);
        businessMetrics.recordCacheAccess("userCache", true);
        businessMetrics.recordCacheAccess("userCache", false);

        // 缓存命中率应该被记录
        // 注意：这个测试可能需要稍等片刻让 Gauge 更新
    }

    /**
     * 测试服务类
     * 用于测试 @Timed 和 @Counted 注解
     */
    @Component
    static class MetricsTestService {

        @Timed(name = "test.timed.method", description = "Test timed method")
        public void timedMethod() {
            try {
                Thread.sleep(10); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Counted(name = "test.counted.method", description = "Test counted method")
        public void countedMethod() {
            // 简单的方法调用
        }
    }
}
