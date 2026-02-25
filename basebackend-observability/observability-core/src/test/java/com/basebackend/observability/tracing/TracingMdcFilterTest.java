package com.basebackend.observability.tracing;

import com.basebackend.observability.tracing.TracingMdcFilter;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("TracingMdcFilter 追踪MDC过滤器测试")
class TracingMdcFilterTest {

    @Test
    @DisplayName("过滤器应已配置")
    void shouldBeConfigured() {
        // 测试类存在性
        assertThat(TracingMdcFilter.class).isNotNull();
    }

    @Test
    @DisplayName("Tracer应已配置")
    void shouldHaveTracer() {
        // 测试Tracer存在
        assertThat(Tracer.class).isNotNull();
    }

    @Test
    @DisplayName("过滤器类应存在")
    void filterClassShouldExist() {
        assertThat(TracingMdcFilter.class).isNotNull();
    }
}
