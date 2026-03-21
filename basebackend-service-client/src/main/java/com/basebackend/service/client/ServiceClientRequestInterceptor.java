package com.basebackend.service.client;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * 服务客户端请求拦截器
 * <p>
 * 在服务间调用时传递认证信息（JWT Token）、链路追踪ID，并标记为内部调用。
 * 替代原 Feign 的 RequestInterceptor，使用 Spring 的 ClientHttpRequestInterceptor。
 * </p>
 *
 * @author Claude Code
 * @since 2025-12-09
 */
public class ServiceClientRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ServiceClientRequestInterceptor.class);

    private static final String X_TRACE_ID = "X-Trace-Id";
    private final String internalRequestSecret;
    private final String serviceName;

    public ServiceClientRequestInterceptor(String internalRequestSecret, String serviceName) {
        this.internalRequestSecret = internalRequestSecret;
        this.serviceName = serviceName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest servletRequest = attributes.getRequest();

            // 传递 Authorization header
            String authorization = servletRequest.getHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                request.getHeaders().set("Authorization", authorization);
                log.trace("服务客户端请求传递 Authorization header");
            }

            // 传递链路追踪 ID
            String traceId = servletRequest.getHeader(X_TRACE_ID);
            if (traceId != null && !traceId.isEmpty()) {
                request.getHeaders().set(X_TRACE_ID, traceId);
            }
        }

        // 标记为内部服务调用
        request.getHeaders().set(InternalRequestAuth.HEADER_INTERNAL_CALL, "true");
        addSignedInternalHeaders(request);

        log.trace("服务客户端请求拦截器处理完成: target={}", request.getURI());

        return execution.execute(request, body);
    }

    private void addSignedInternalHeaders(HttpRequest request) {
        if (!StringUtils.hasText(internalRequestSecret) || !StringUtils.hasText(serviceName)) {
            log.debug("跳过内部请求签名: serviceName 或共享密钥未配置");
            return;
        }

        long timestamp = System.currentTimeMillis();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";
        String path = request.getURI() != null ? request.getURI().getPath() : "/";
        String signature = InternalRequestAuth.sign(internalRequestSecret, serviceName, timestamp, method, path);

        request.getHeaders().set(InternalRequestAuth.HEADER_SERVICE_NAME, serviceName);
        request.getHeaders().set(InternalRequestAuth.HEADER_TIMESTAMP, String.valueOf(timestamp));
        request.getHeaders().set(InternalRequestAuth.HEADER_SIGNATURE, signature);
    }
}
