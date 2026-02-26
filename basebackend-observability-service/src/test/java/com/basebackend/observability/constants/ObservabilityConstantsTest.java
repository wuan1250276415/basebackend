package com.basebackend.observability.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ObservabilityConstants 单元测试
 */
class ObservabilityConstantsTest {

    @Test
    @DisplayName("常量类不可实例化")
    void shouldNotBeInstantiable() throws Exception {
        Constructor<ObservabilityConstants> constructor = ObservabilityConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    @DisplayName("时间常量")
    class TimeConstants {

        @Test
        @DisplayName("时间常量值正确")
        void shouldHaveCorrectTimeValues() {
            assertThat(ObservabilityConstants.ONE_MINUTE_MS).isEqualTo(60_000L);
            assertThat(ObservabilityConstants.ONE_HOUR_MS).isEqualTo(3_600_000L);
            assertThat(ObservabilityConstants.ONE_DAY_MS).isEqualTo(86_400_000L);
        }

        @Test
        @DisplayName("时间常量关系正确")
        void shouldHaveCorrectRelationship() {
            assertThat(ObservabilityConstants.ONE_HOUR_MS)
                    .isEqualTo(ObservabilityConstants.ONE_MINUTE_MS * 60);
            assertThat(ObservabilityConstants.ONE_DAY_MS)
                    .isEqualTo(ObservabilityConstants.ONE_HOUR_MS * 24);
        }
    }

    @Nested
    @DisplayName("API 路径常量")
    class ApiPaths {

        @Test
        @DisplayName("Zipkin 路径以 / 开头")
        void zipkinPathsShouldStartWithSlash() {
            assertThat(ObservabilityConstants.ZIPKIN_API_V2).startsWith("/");
            assertThat(ObservabilityConstants.ZIPKIN_TRACES_PATH).startsWith("/");
            assertThat(ObservabilityConstants.ZIPKIN_SERVICES_PATH).startsWith("/");
            assertThat(ObservabilityConstants.ZIPKIN_SPANS_PATH).startsWith("/");
        }

        @Test
        @DisplayName("Prometheus 路径以 / 开头")
        void prometheusPathsShouldStartWithSlash() {
            assertThat(ObservabilityConstants.PROMETHEUS_QUERY_PATH).startsWith("/");
            assertThat(ObservabilityConstants.PROMETHEUS_QUERY_RANGE_PATH).startsWith("/");
        }

        @Test
        @DisplayName("Loki 路径以 / 开头")
        void lokiPathsShouldStartWithSlash() {
            assertThat(ObservabilityConstants.LOKI_QUERY_PATH).startsWith("/");
            assertThat(ObservabilityConstants.LOKI_QUERY_RANGE_PATH).startsWith("/");
        }
    }

    @Nested
    @DisplayName("默认值常量")
    class DefaultValues {

        @Test
        @DisplayName("默认值合理")
        void shouldHaveReasonableDefaults() {
            assertThat(ObservabilityConstants.DEFAULT_LIMIT).isEqualTo(100);
            assertThat(ObservabilityConstants.DEFAULT_LOG_LIMIT).isEqualTo(1000);
            assertThat(ObservabilityConstants.MAX_LIMIT).isEqualTo(10000);
            assertThat(ObservabilityConstants.DEFAULT_STEP_SECONDS).isEqualTo(60);
        }

        @Test
        @DisplayName("DEFAULT_LIMIT < MAX_LIMIT")
        void defaultShouldBeLessThanMax() {
            assertThat(ObservabilityConstants.DEFAULT_LIMIT)
                    .isLessThan(ObservabilityConstants.MAX_LIMIT);
            assertThat(ObservabilityConstants.DEFAULT_LOG_LIMIT)
                    .isLessThanOrEqualTo(ObservabilityConstants.MAX_LIMIT);
        }
    }

    @Nested
    @DisplayName("缓存名称常量")
    class CacheNames {

        @Test
        @DisplayName("缓存名称以 obs- 前缀")
        void shouldHaveObsPrefix() {
            assertThat(ObservabilityConstants.CACHE_SERVICES).startsWith("obs-");
            assertThat(ObservabilityConstants.CACHE_TRACES).startsWith("obs-");
            assertThat(ObservabilityConstants.CACHE_METRICS).startsWith("obs-");
            assertThat(ObservabilityConstants.CACHE_LOGS).startsWith("obs-");
        }

        @Test
        @DisplayName("缓存名称互不重复")
        void shouldBeUnique() {
            var names = java.util.Set.of(
                    ObservabilityConstants.CACHE_SERVICES,
                    ObservabilityConstants.CACHE_TRACES,
                    ObservabilityConstants.CACHE_METRICS,
                    ObservabilityConstants.CACHE_LOGS
            );
            assertThat(names).hasSize(4);
        }
    }

    @Nested
    @DisplayName("错误消息常量")
    class ErrorMessages {

        @Test
        @DisplayName("错误消息非空")
        void shouldNotBeEmpty() {
            assertThat(ObservabilityConstants.ERROR_SERVICE_UNAVAILABLE).isNotBlank();
            assertThat(ObservabilityConstants.ERROR_QUERY_TIMEOUT).isNotBlank();
            assertThat(ObservabilityConstants.ERROR_INVALID_PARAMS).isNotBlank();
        }
    }
}
