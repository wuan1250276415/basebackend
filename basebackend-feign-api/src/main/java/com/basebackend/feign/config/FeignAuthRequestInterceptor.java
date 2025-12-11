package com.basebackend.feign.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器
 * 用于在服务间调用时传递认证信息（如 JWT Token）
 *
 * @author Claude Code
 * @since 2025-12-09
 */
@Slf4j
@Configuration
public class FeignAuthRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String X_TRACE_ID = "X-Trace-Id";
    private static final String X_INTERNAL_CALL = "X-Internal-Call";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 传递 Authorization header
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            if (authorization != null && !authorization.isEmpty()) {
                template.header(AUTHORIZATION_HEADER, authorization);
                log.trace("Feign 请求传递 Authorization header");
            }

            // 传递链路追踪 ID
            String traceId = request.getHeader(X_TRACE_ID);
            if (traceId != null && !traceId.isEmpty()) {
                template.header(X_TRACE_ID, traceId);
            }
        }

        // 标记为内部服务调用
        template.header(X_INTERNAL_CALL, "true");

        log.trace("Feign 请求拦截器处理完成: target={}", template.url());
    }
}
