package com.basebackend.system.security;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.service.client.InternalRequestAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校验内部服务调用签名，防止内部路由被外部用户直接复用。
 */
@Component
public class InternalRequestAuthValidator {

    private final String sharedSecret;
    private final long maxSkewMs;
    private final Set<String> allowedServices;

    public InternalRequestAuthValidator(
            @Value("${jwt.secret:}") String sharedSecret,
            @Value("${basebackend.internal.operation-log.max-skew-ms:300000}") long maxSkewMs,
            @Value("${basebackend.internal.operation-log.allowed-callers:basebackend-user-api}") String allowedServices) {
        this.sharedSecret = sharedSecret;
        this.maxSkewMs = maxSkewMs;
        this.allowedServices = Arrays.stream(allowedServices.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void requireValid(HttpServletRequest request) {
        if (!StringUtils.hasText(sharedSecret)) {
            throw new BusinessException("内部服务鉴权未配置");
        }

        String serviceName = request.getHeader(InternalRequestAuth.HEADER_SERVICE_NAME);
        String timestampHeader = request.getHeader(InternalRequestAuth.HEADER_TIMESTAMP);
        String signature = request.getHeader(InternalRequestAuth.HEADER_SIGNATURE);
        String internalCall = request.getHeader(InternalRequestAuth.HEADER_INTERNAL_CALL);

        if (!"true".equalsIgnoreCase(internalCall)
                || !StringUtils.hasText(serviceName)
                || !StringUtils.hasText(timestampHeader)
                || !StringUtils.hasText(signature)) {
            throw new BusinessException("非法内部调用");
        }
        if (!allowedServices.isEmpty() && !allowedServices.contains(serviceName)) {
            throw new BusinessException("内部调用方未授权");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException e) {
            throw new BusinessException("内部调用时间戳非法");
        }

        long skew = Math.abs(System.currentTimeMillis() - timestamp);
        if (skew > maxSkewMs) {
            throw new BusinessException("内部调用签名已过期");
        }

        boolean verified = InternalRequestAuth.verify(
                sharedSecret,
                serviceName,
                timestamp,
                request.getMethod(),
                request.getRequestURI(),
                signature
        );
        if (!verified) {
            throw new BusinessException("内部调用签名校验失败");
        }
    }
}
