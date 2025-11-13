package com.basebackend.observability.tracing;

import brave.Tracer;
import brave.propagation.TraceContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MDC 追踪过滤器
 * 将 TraceId 和 SpanId 自动注入到 MDC 中，使所有日志都包含链路追踪信息
 *
 * MDC (Mapped Diagnostic Context) 是 SLF4J 提供的机制，可以将上下文信息附加到日志中
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)  // 在 TracingFilter 之前执行
@RequiredArgsConstructor
public class TracingMdcFilter implements Filter {

    private final Tracer tracer;

    // MDC 键名
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    public static final String REQUEST_ID_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            if (request instanceof HttpServletRequest httpRequest) {
                // 获取当前 Span 的 TraceContext
                TraceContext context = tracer.currentSpan() != null ?
                        tracer.currentSpan().context() : null;

                if (context != null) {
                    String traceId = context.traceIdString();
                    String spanId = context.spanIdString();

                    // 将 TraceId 和 SpanId 注入到 MDC
                    MDC.put(TRACE_ID_KEY, traceId);
                    MDC.put(SPAN_ID_KEY, spanId);
                    MDC.put(REQUEST_ID_KEY, generateRequestId(traceId, spanId));

                    log.debug("MDC initialized with traceId={}, spanId={} for URI={}",
                            traceId, spanId, httpRequest.getRequestURI());

                    // 同时添加到响应头，方便前端追踪
                    if (response instanceof HttpServletResponse httpResponse) {
                        httpResponse.setHeader("X-Trace-Id", traceId);
                        httpResponse.setHeader("X-Span-Id", spanId);
                        httpResponse.setHeader("X-Request-Id", MDC.get(REQUEST_ID_KEY));
                    }
                } else {
                    // 即使没有 Trace Context，也生成一个临时的 RequestId
                    String requestId = generateFallbackRequestId();
                    MDC.put(REQUEST_ID_KEY, requestId);
                    log.debug("No trace context available, using fallback requestId={}", requestId);
                }
            }

            // 继续过滤器链
            chain.doFilter(request, response);

        } finally {
            // 清理 MDC，避免内存泄漏（特别是在使用线程池的环境中）
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
            MDC.remove(REQUEST_ID_KEY);
        }
    }

    /**
     * 生成请求 ID
     * 格式：{traceId}-{spanId}
     */
    private String generateRequestId(String traceId, String spanId) {
        return traceId + "-" + spanId;
    }

    /**
     * 生成备用请求 ID（当没有 Trace Context 时）
     * 格式：{timestamp}-{random}
     */
    private String generateFallbackRequestId() {
        return System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }
}
