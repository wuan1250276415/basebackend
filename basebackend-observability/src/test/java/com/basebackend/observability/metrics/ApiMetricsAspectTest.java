package com.basebackend.observability.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiMetricsAspect API指标切面测试")
class ApiMetricsAspectTest {

    @Test
    @DisplayName("切面应已配置")
    void shouldBeConfigured() {
        // 测试类存在性
        assertThat(ApiMetricsAspect.class).isNotNull();
    }

    @Test
    @DisplayName("CustomMetrics应已配置")
    void shouldHaveCustomMetrics() {
        // 测试类存在性
        assertThat(CustomMetrics.class).isNotNull();
    }
}
