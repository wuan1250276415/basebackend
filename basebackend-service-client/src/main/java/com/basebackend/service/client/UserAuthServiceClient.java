package com.basebackend.service.client;

import com.basebackend.api.model.user.LoginResponse;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 用户认证服务客户端（微信登录等）
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/user/auth")
public interface UserAuthServiceClient {

    @PostExchange("/auth/wechat-login")
    @Operation(summary = "微信单点登录")
    Result<LoginResponse> wechatLogin(@RequestParam("phone") String phone);
}
