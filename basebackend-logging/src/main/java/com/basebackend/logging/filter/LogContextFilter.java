package com.basebackend.logging.filter;

import com.basebackend.logging.context.LogContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 日志上下文过滤器
 * 为每个HTTP请求自动添加 TraceId、RequestId 等上下文信息
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 初始化日志上下文
            LogContext.init();

            // 尝试从请求头获取 TraceId（用于分布式追踪）
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId != null && !traceId.isEmpty()) {
                LogContext.setTraceId(traceId);
            }

            // 尝试从请求头获取 RequestId
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId != null && !requestId.isEmpty()) {
                LogContext.setRequestId(requestId);
            }

            // 设置请求信息
            LogContext.setIpAddress(getClientIpAddress(request));
            LogContext.setUri(request.getRequestURI());
            LogContext.setMethod(request.getMethod());

            // 将 TraceId 和 RequestId 添加到响应头
            response.setHeader(TRACE_ID_HEADER, LogContext.getTraceId());
            response.setHeader(REQUEST_ID_HEADER, LogContext.getRequestId());

            // 继续过滤器链
            filterChain.doFilter(request, response);

        } finally {
            // 清除日志上下文，防止内存泄漏
            LogContext.clear();
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多次反向代理后会有多个IP值，第一个为真实IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
