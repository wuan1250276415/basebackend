package com.basebackend.auth.sentinel;

import com.basebackend.auth.dto.LoginRequest;
import com.basebackend.auth.dto.LoginResponse;
import com.basebackend.common.model.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 熔断降级处理类
 */
@Slf4j
public class SentinelFallbackHandler {

    /**
     * 登录熔断处理
     */
    public static Result<LoginResponse> handleLoginFallback(LoginRequest loginRequest, Throwable e) {
        log.error("登录接口发生熔断: {}", loginRequest.getUsername(), e);
        return Result.error("服务暂时不可用，请稍后重试");
    }

    /**
     * 通用熔断处理
     */
    public static Result<String> handleFallback(Throwable e) {
        log.error("接口发生熔断: {}", e.getMessage(), e);
        return Result.error("服务暂时不可用，请稍后重试");
    }
}
