package com.basebackend.auth.sentinel;

import com.basebackend.auth.dto.LoginRequest;
import com.basebackend.auth.dto.LoginResponse;
import com.basebackend.auth.dto.PasswordChangeDTO;
import com.basebackend.common.model.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 限流降级处理类
 */
@Slf4j
public class SentinelBlockHandler {

    /**
     * 登录限流处理
     */
    public static Result<LoginResponse> handleLoginBlock(LoginRequest loginRequest, Throwable e) {
        log.warn("登录接口触发限流: {}", loginRequest.getUsername());
        return Result.error("系统繁忙，请稍后重试");
    }

    /**
     * 通用限流处理
     */
    public static Result<String> handleBlock(Throwable e) {
        log.warn("接口触发限流: {}", e.getMessage());
        return Result.error("系统繁忙，请稍后重试");
    }
}
