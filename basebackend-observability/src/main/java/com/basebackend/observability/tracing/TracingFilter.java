package com.basebackend.observability.tracing;

import brave.Tracer;
import brave.propagation.TraceContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 追踪过滤器
 * 自动为每个请求创建 Span，并将 TraceId 添加到响应头
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class TracingFilter implements Filter {

    private final Tracer tracer;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 获取当前 Span 的 TraceContext
            TraceContext context = tracer.currentSpan() != null ?
                    tracer.currentSpan().context() : null;

            if (context != null) {
                String traceId = context.traceIdString();
                String spanId = context.spanIdString();

                // 将 TraceId 和 SpanId 添加到响应头
                httpResponse.setHeader("X-Trace-Id", traceId);
                httpResponse.setHeader("X-Span-Id", spanId);

                // 记录追踪信息
                log.debug("Processing request with TraceId: {}, SpanId: {}, URI: {}",
                        traceId, spanId, httpRequest.getRequestURI());
            }
        }

        chain.doFilter(request, response);
    }
}
