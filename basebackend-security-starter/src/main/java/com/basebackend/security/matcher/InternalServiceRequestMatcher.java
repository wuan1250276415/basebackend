package com.basebackend.security.matcher;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 内部服务调用请求匹配器
 * 用于识别带有 X-Internal-Call header 的内部服务调用
 *
 * @author Claude Code
 * @since 2025-12-09
 */
public class InternalServiceRequestMatcher implements RequestMatcher {

    private static final String X_INTERNAL_CALL = "X-Internal-Call";

    @Override
    public boolean matches(HttpServletRequest request) {
        String internalCall = request.getHeader(X_INTERNAL_CALL);
        return "true".equalsIgnoreCase(internalCall);
    }
}
