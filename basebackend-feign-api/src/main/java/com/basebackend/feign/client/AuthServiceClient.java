package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 认证服务Feign客户端
 */
@FeignClient(name = "basebackend-auth-api", path = "/api/auth")
public interface AuthServiceClient {

    /**
     * 验证Token
     */
    @GetMapping("/verify")
    Result<Boolean> verifyToken(@RequestParam("token") String token);

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    Result<Object> refreshToken(@RequestParam("refreshToken") String refreshToken);

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    Result<Object> getCurrentUserInfo();
}
