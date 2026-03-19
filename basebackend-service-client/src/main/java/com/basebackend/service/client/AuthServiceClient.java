package com.basebackend.service.client;

import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 用户认证服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/user/auth")
public interface AuthServiceClient {

    @GetExchange("/verify")
    @Operation(summary = "验证Token")
    Result<Boolean> verifyToken(@RequestParam("token") String token);

    @PostExchange("/refresh")
    @Operation(summary = "刷新Token")
    Result<Object> refreshToken(@RequestParam("refreshToken") String refreshToken);

    @GetExchange("/info")
    @Operation(summary = "获取当前用户信息")
    Result<Object> getCurrentUserInfo();
}
