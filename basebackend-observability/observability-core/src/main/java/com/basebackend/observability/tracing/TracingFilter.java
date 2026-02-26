package com.basebackend.observability.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 追踪过滤器
 * 将当前 Span 的 TraceId/SpanId 添加到 HTTP 响应头，方便前端和调试追踪。
 * <p>
 * 使用 Micrometer Tracing API（通过 OTel bridge），不再直接依赖 Brave。
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@ConditionalOnClass(Tracer.class)
@ConditionalOnBean(Tracer.class)
public class TracingFilter implements Filter {

    private final Tracer tracer;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {

            Span currentSpan = tracer.currentSpan();

            if (currentSpan != null) {
                String traceId = currentSpan.context().traceId();
                String spanId = currentSpan.context().spanId();

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
