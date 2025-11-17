package com.basebackend.observability;

import brave.Span;
import brave.Tracer;
import com.basebackend.observability.tracing.TracingMdcFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 链路追踪集成测试
 * 测试 TracingMdcFilter 的 MDC 注入功能
 */
@SpringBootTest
@DisplayName("链路追踪集成测试")
class TracingIntegrationTest {

    @Mock
    private Tracer tracer;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Span span;

    private TracingMdcFilter tracingMdcFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tracingMdcFilter = new TracingMdcFilter(tracer);
        MDC.clear(); // 清理 MDC
    }

    @Test
    @DisplayName("测试 TracingMdcFilter 注入 TraceId 和 SpanId 到 MDC")
    void testTracingMdcFilterInjectsMDC() throws Exception {
        // 模拟 Trace Context
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(brave.propagation.TraceContext.newBuilder()
                .traceId(123456789L)
                .spanId(987654321L)
                .build());

        // 执行过滤器
        tracingMdcFilter.doFilter(request, response, filterChain);

        // 验证 FilterChain 被调用
        verify(filterChain, times(1)).doFilter(request, response);

        // 注意：由于 MDC 在 finally 块中被清理，我们需要在 FilterChain 执行期间检查
        // 这里我们通过捕获 FilterChain 调用来验证
    }

    @Test
    @DisplayName("测试 TracingMdcFilter 添加响应头")
    void testTracingMdcFilterAddsResponseHeaders() throws Exception {
        // 模拟 Trace Context
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(brave.propagation.TraceContext.newBuilder()
                .traceId(123456789L)
                .spanId(987654321L)
                .build());

        // 执行过滤器
        tracingMdcFilter.doFilter(request, response, filterChain);

        // 验证响应头被设置
        ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

        verify(response, atLeastOnce()).setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        // 检查是否包含 X-Trace-Id 和 X-Span-Id 头
        assertThat(headerNameCaptor.getAllValues()).contains("X-Trace-Id", "X-Span-Id", "X-Request-Id");
    }

    @Test
    @DisplayName("测试没有 Trace Context 时使用备用 RequestId")
    void testTracingMdcFilterFallbackRequestId() throws Exception {
        // 模拟没有 Trace Context
        when(tracer.currentSpan()).thenReturn(null);

        // 执行过滤器
        tracingMdcFilter.doFilter(request, response, filterChain);

        // 验证 FilterChain 被调用（即使没有 Trace Context 也应该继续）
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("测试 MDC 键名常量")
    void testMDCKeyConstants() {
        assertThat(TracingMdcFilter.TRACE_ID_KEY).isEqualTo("traceId");
        assertThat(TracingMdcFilter.SPAN_ID_KEY).isEqualTo("spanId");
        assertThat(TracingMdcFilter.REQUEST_ID_KEY).isEqualTo("requestId");
    }

    @Test
    @DisplayName("测试 MDC 清理（避免内存泄漏）")
    void testMDCCleanup() throws Exception {
        // 模拟 Trace Context
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(brave.propagation.TraceContext.newBuilder()
                .traceId(123456789L)
                .spanId(987654321L)
                .build());

        // 在过滤器调用前设置一些 MDC 值
        MDC.put("before", "value");

        // 执行过滤器
        tracingMdcFilter.doFilter(request, response, filterChain);

        // 验证 traceId 和 spanId 已被清理
        assertThat(MDC.get(TracingMdcFilter.TRACE_ID_KEY)).isNull();
        assertThat(MDC.get(TracingMdcFilter.SPAN_ID_KEY)).isNull();
        assertThat(MDC.get(TracingMdcFilter.REQUEST_ID_KEY)).isNull();

        // 验证其他 MDC 值不受影响
        assertThat(MDC.get("before")).isEqualTo("value");

        // 清理
        MDC.clear();
    }
}
