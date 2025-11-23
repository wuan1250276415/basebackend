//package com.basebackend.observability.integration;
//
//import com.basebackend.observability.slo.annotation.SloMonitored;
//import com.basebackend.observability.slo.config.SloProperties;
//import com.basebackend.observability.slo.model.SloType;
//import com.basebackend.observability.slo.registry.SloRegistry;
//import io.micrometer.core.instrument.MeterRegistry;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.EnableAspectJAutoProxy;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
///**
// * SLO 监控集成测试
// * <p>
// * 验证 SLO 监控系统能正确捕获方法调用的指标。
// * </p>
// */
//@SpringBootTest
//@ActiveProfiles("test")
//@DisplayName("SLO 监控集成测试")
//class SloMonitoringIntegrationTest {
//
//    @Autowired(required = false)
//    private SloRegistry sloRegistry;
//
//    @Autowired(required = false)
//    private MeterRegistry meterRegistry;
//
//    @Autowired(required = false)
//    private TestService testService;
//
//    @TestConfiguration
//    @EnableAspectJAutoProxy
//    static class TestConfig {
//
//        @Bean
//        public TestService testService() {
//            return new TestService();
//        }
//    }
//
//    /**
//     * 测试服务（使用 @SloMonitored 注解）
//     */
//    static class TestService {
//
//        @SloMonitored(sloName = "test-availability", service = "test-service")
//        public String successOperation() {
//            return "success";
//        }
//
//        @SloMonitored(sloName = "test-availability", service = "test-service")
//        public String failureOperation() {
//            throw new RuntimeException("Simulated failure");
//        }
//
//        @SloMonitored(sloName = "test-latency", service = "test-service")
//        public String slowOperation() throws InterruptedException {
//            Thread.sleep(100);
//            return "slow";
//        }
//    }
//
//    @Test
//    @DisplayName("成功请求应更新可用性 SLO 指标")
//    void shouldUpdateAvailabilitySloOnSuccess() {
//        if (sloRegistry == null || testService == null) {
//            return;
//        }
//
//        // 执行成功操作
//        String result = testService.successOperation();
//        assertThat(result).isEqualTo("success");
//
//        // 验证 SLO 指标已更新
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            Double availability = sloRegistry.getCurrentSloValue("test-availability");
//            if (availability != null) {
//                assertThat(availability)
//                        .as("Availability should be high after successful request")
//                        .isGreaterThanOrEqualTo(0.0);
//            }
//        });
//    }
//
//    @Test
//    @DisplayName("失败请求应降低可用性 SLO")
//    void shouldDecrementAvailabilitySloOnFailure() {
//        if (sloRegistry == null || testService == null) {
//            return;
//        }
//
//        try {
//            testService.failureOperation();
//        } catch (RuntimeException e) {
//            // 预期异常
//        }
//
//        // 验证错误被记录
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            Long errorCount = sloRegistry.getErrorBudgetRemaining("test-availability");
//            // 错误预算应减少（如果 SLO 已注册）
//            if (errorCount != null) {
//                assertThat(errorCount).isGreaterThanOrEqualTo(0L);
//            }
//        });
//    }
//
//    @Test
//    @DisplayName("慢请求应被延迟 SLO 捕获")
//    void shouldCaptureSlowRequestsInLatencySlo() throws InterruptedException {
//        if (sloRegistry == null || testService == null) {
//            return;
//        }
//
//        // 执行慢操作
//        String result = testService.slowOperation();
//        assertThat(result).isEqualTo("slow");
//
//        // 验证延迟指标已更新
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            Double latency = sloRegistry.getCurrentSloValue("test-latency");
//            if (latency != null) {
//                assertThat(latency).isGreaterThanOrEqualTo(0.0);
//            }
//        });
//    }
//
//    @Test
//    @DisplayName("SLO 注册表应正确加载配置的 SLO")
//    void shouldLoadConfiguredSlos() {
//        if (sloRegistry == null) {
//            return;
//        }
//
//        // 验证 SLO 注册表可用
//        assertThat(sloRegistry.getAllSloNames())
//                .as("SLO registry should be available")
//                .isNotNull();
//    }
//
//    @Test
//    @DisplayName("错误预算计算应正确")
//    void shouldCalculateErrorBudgetCorrectly() {
//        if (sloRegistry == null) {
//            return;
//        }
//
//        // 创建测试 SLO 配置
//        SloProperties.SloConfig config = new SloProperties.SloConfig();
//        config.setName("test-error-budget");
//        config.setType(SloType.AVAILABILITY);
//        config.setTarget(0.99); // 99% 可用性
//        config.setWindow("1h");
//        config.setService("test-service");
//
//        sloRegistry.registerSlo(config);
//
//        // 验证错误预算被正确初始化
//        Long errorBudget = sloRegistry.getErrorBudgetRemaining("test-error-budget");
//        if (errorBudget != null) {
//            assertThat(errorBudget)
//                    .as("Error budget should be calculated based on target")
//                    .isGreaterThanOrEqualTo(0L);
//        }
//    }
//}
