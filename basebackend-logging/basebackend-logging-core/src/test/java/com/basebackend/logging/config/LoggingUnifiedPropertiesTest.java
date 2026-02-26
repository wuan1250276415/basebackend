package com.basebackend.logging.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoggingUnifiedProperties 单元测试
 */
class LoggingUnifiedPropertiesTest {

    @Nested
    @DisplayName("默认值")
    class Defaults {

        @Test
        @DisplayName("模块默认启用")
        void shouldBeEnabledByDefault() {
            var props = new LoggingUnifiedProperties();
            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("performance 默认配置合理")
        void shouldHaveReasonablePerformanceDefaults() {
            var perf = new LoggingUnifiedProperties.PerformanceConfig();
            assertThat(perf.getAsyncPoolSize()).isGreaterThan(0);
            assertThat(perf.getAsyncQueueCapacity()).isEqualTo(10000);
            assertThat(perf.isEnableMetrics()).isTrue();
            assertThat(perf.getSlowThresholdMs()).isEqualTo(100);
            assertThat(perf.getMemoryAlertThreshold()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("monitoring 默认配置合理")
        void shouldHaveReasonableMonitoringDefaults() {
            var mon = new LoggingUnifiedProperties.MonitoringConfig();
            assertThat(mon.isEnableHealthCheck()).isTrue();
            assertThat(mon.getHealthCheckIntervalSeconds()).isEqualTo(30);
            assertThat(mon.isEnableAlerts()).isTrue();
            assertThat(mon.isEnablePrometheus()).isTrue();
        }
    }

    @Nested
    @DisplayName("PerformanceConfig 校验")
    class PerformanceValidation {

        @Test
        @DisplayName("正常配置通过校验")
        void shouldPassValidConfig() {
            var perf = new LoggingUnifiedProperties.PerformanceConfig();
            perf.validate(); // should not throw
        }

        @Test
        @DisplayName("asyncPoolSize < 1 抛异常")
        void shouldThrowForZeroPoolSize() {
            var perf = new LoggingUnifiedProperties.PerformanceConfig();
            perf.setAsyncPoolSize(0);
            assertThatThrownBy(perf::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("线程池");
        }

        @Test
        @DisplayName("asyncQueueCapacity < 100 抛异常")
        void shouldThrowForSmallQueue() {
            var perf = new LoggingUnifiedProperties.PerformanceConfig();
            perf.setAsyncQueueCapacity(50);
            assertThatThrownBy(perf::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("队列");
        }

        @Test
        @DisplayName("slowThresholdMs < 1 抛异常")
        void shouldThrowForZeroThreshold() {
            var perf = new LoggingUnifiedProperties.PerformanceConfig();
            perf.setSlowThresholdMs(0);
            assertThatThrownBy(perf::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("慢操作");
        }

        @Test
        @DisplayName("memoryAlertThreshold 超出范围抛异常")
        void shouldThrowForInvalidMemoryThreshold() {
            var perf = new LoggingUnifiedProperties.PerformanceConfig();
            perf.setMemoryAlertThreshold(0.3);
            assertThatThrownBy(perf::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("内存");

            perf.setMemoryAlertThreshold(1.0);
            assertThatThrownBy(perf::validate)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("MonitoringConfig 校验")
    class MonitoringValidation {

        @Test
        @DisplayName("正常配置通过校验")
        void shouldPassValidConfig() {
            var mon = new LoggingUnifiedProperties.MonitoringConfig();
            mon.validate(); // should not throw
        }

        @Test
        @DisplayName("healthCheckIntervalSeconds < 5 抛异常")
        void shouldThrowForTooFrequentHealthCheck() {
            var mon = new LoggingUnifiedProperties.MonitoringConfig();
            mon.setHealthCheckIntervalSeconds(3);
            assertThatThrownBy(mon::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("健康检查");
        }
    }

    @Nested
    @DisplayName("validateAll")
    class ValidateAll {

        @Test
        @DisplayName("禁用时跳过校验")
        void shouldSkipWhenDisabled() {
            var props = new LoggingUnifiedProperties();
            props.setEnabled(false);
            props.getPerformance().setAsyncPoolSize(0); // 无效值
            props.validateAll(); // 禁用时不抛异常
        }

        @Test
        @DisplayName("启用时触发子配置校验")
        void shouldValidateSubConfigsWhenEnabled() {
            var props = new LoggingUnifiedProperties();
            props.setEnabled(true);
            props.getPerformance().setAsyncPoolSize(0);
            assertThatThrownBy(props::validateAll)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
