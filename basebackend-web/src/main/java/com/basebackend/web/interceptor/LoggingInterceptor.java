package com.basebackend.web.interceptor;

import com.basebackend.common.util.IpUtil;
import com.basebackend.common.util.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 请求日志拦截器
 * 记录HTTP请求的详细信息，包括请求路径、方法、耗时、IP等
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_KEY = "logging_interceptor_start_time";
    private static final String REQUEST_ID_KEY = "logging_interceptor_request_id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_KEY, startTime);

        // 生成请求ID
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_KEY, requestId);

        // 记录请求基本信息
        log.info("""
                Request Started - [{}]
                ID: {}
                Method: {} {}
                IP: {}
                User-Agent: {}
                """,
                LocalDateTime.now(),
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                IpUtil.getIpAddress(request),
                UserAgentUtil.getBrowser(request));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        long startTime = (Long) request.getAttribute(START_TIME_KEY);
        long duration = System.currentTimeMillis() - startTime;
        String requestId = (String) request.getAttribute(REQUEST_ID_KEY);

        // 记录响应信息
        if (ex != null) {
            log.error("""
                    Request Failed - [{}]
                    ID: {}
                    Status: {}
                    Duration: {}ms
                    Error: {}
                    """,
                    LocalDateTime.now(),
                    requestId,
                    response.getStatus(),
                    duration,
                    ex.getMessage(),
                    ex);
        } else {
            log.info("""
                    Request Completed - [{}]
                    ID: {}
                    Status: {}
                    Duration: {}ms
                    """,
                    LocalDateTime.now(),
                    requestId,
                    response.getStatus(),
                    duration);
        }
    }
}
