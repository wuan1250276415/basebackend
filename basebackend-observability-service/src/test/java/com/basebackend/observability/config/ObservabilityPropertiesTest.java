package com.basebackend.observability.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObservabilityProperties 单元测试
 */
class ObservabilityPropertiesTest {

    @Test
    @DisplayName("默认配置创建成功")
    void shouldCreateWithDefaults() {
        var props = new ObservabilityProperties();
        assertThat(props.getTrace()).isNotNull();
        assertThat(props.getMetrics()).isNotNull();
        assertThat(props.getLogs()).isNotNull();
        assertThat(props.getCache()).isNotNull();
        assertThat(props.getHttpClient()).isNotNull();
    }

    @Nested
    @DisplayName("Trace 配置")
    class TraceConfig {

        @Test
        @DisplayName("默认值正确")
        void shouldHaveCorrectDefaults() {
            var trace = new ObservabilityProperties.Trace();
            assertThat(trace.getEndpoint()).isEqualTo("http://localhost:9411");
            assertThat(trace.getFormat()).isEqualTo("zipkin");
            assertThat(trace.getDefaultLookbackMs()).isEqualTo(3600000L);
            assertThat(trace.getDefaultLimit()).isEqualTo(100);
        }

        @Test
        @DisplayName("setter/getter 正常工作")
        void shouldSetAndGet() {
            var trace = new ObservabilityProperties.Trace();
            trace.setEndpoint("http://tempo:3200");
            trace.setFormat("tempo");
            trace.setDefaultLookbackMs(7200000L);
            trace.setDefaultLimit(50);

            assertThat(trace.getEndpoint()).isEqualTo("http://tempo:3200");
            assertThat(trace.getFormat()).isEqualTo("tempo");
            assertThat(trace.getDefaultLookbackMs()).isEqualTo(7200000L);
            assertThat(trace.getDefaultLimit()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Metrics 配置")
    class MetricsConfig {

        @Test
        @DisplayName("默认值正确")
        void shouldHaveCorrectDefaults() {
            var metrics = new ObservabilityProperties.Metrics();
            assertThat(metrics.getEndpoint()).isEqualTo("http://localhost:9090");
            assertThat(metrics.getDefaultStep()).isEqualTo(60);
            assertThat(metrics.getDefaultRange()).isEqualTo(3600);
        }
    }

    @Nested
    @DisplayName("Logs 配置")
    class LogsConfig {

        @Test
        @DisplayName("默认值正确")
        void shouldHaveCorrectDefaults() {
            var logs = new ObservabilityProperties.Logs();
            assertThat(logs.getEndpoint()).isEqualTo("http://localhost:3100");
            assertThat(logs.getDefaultLimit()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Cache 配置")
    class CacheConfig {

        @Test
        @DisplayName("默认启用缓存")
        void shouldBeEnabledByDefault() {
            var cache = new ObservabilityProperties.Cache();
            assertThat(cache.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认 TTL 值合理")
        void shouldHaveReasonableTtlDefaults() {
            var cache = new ObservabilityProperties.Cache();
            assertThat(cache.getServicesTtl()).isEqualTo(60);
            assertThat(cache.getTracesTtl()).isEqualTo(300);
            assertThat(cache.getMetricsTtl()).isEqualTo(30);
            // metrics TTL < traces TTL (指标数据更新更频繁)
            assertThat(cache.getMetricsTtl()).isLessThan(cache.getTracesTtl());
        }
    }

    @Nested
    @DisplayName("HttpClient 配置")
    class HttpClientConfig {

        @Test
        @DisplayName("默认值正确")
        void shouldHaveCorrectDefaults() {
            var http = new ObservabilityProperties.HttpClient();
            assertThat(http.getConnectTimeout()).isEqualTo(5);
            assertThat(http.getReadTimeout()).isEqualTo(30);
            assertThat(http.getMaxConnections()).isEqualTo(200);
            assertThat(http.getMaxConnectionsPerRoute()).isEqualTo(50);
            assertThat(http.isRetryEnabled()).isTrue();
            assertThat(http.getMaxRetries()).isEqualTo(3);
        }

        @Test
        @DisplayName("maxConnectionsPerRoute < maxConnections")
        void perRouteShouldBeLessThanTotal() {
            var http = new ObservabilityProperties.HttpClient();
            assertThat(http.getMaxConnectionsPerRoute()).isLessThan(http.getMaxConnections());
        }
    }
}
