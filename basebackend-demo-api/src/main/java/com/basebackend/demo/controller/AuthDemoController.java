package com.basebackend.demo.controller;

import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthDemoController {

    private final JwtUtil jwtUtil;

    /**
     * 登录演示 - 生成JWT Token
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String username, @RequestParam String password) {
        log.info("用户登录: {}", username);

        // 简单演示，实际应该验证用户名密码
        if ("admin".equals(username) && "123456".equals(password)) {
            // 生成Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username);
            claims.put("role", "ADMIN");

            String token = jwtUtil.generateToken(username, claims);

            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("username", username);

            return Result.success(data);
        }

        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 验证Token
     */
    @GetMapping("/validate")
    public Result<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(401, "Token格式错误");
        }

        String token = authorization.substring(7);
        boolean valid = jwtUtil.validateToken(token);

        if (valid) {
            String subject = jwtUtil.getSubjectFromToken(token);
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("subject", subject);
            data.put("claims", jwtUtil.getClaimsFromToken(token));
            return Result.success(data);
        }

        return Result.error(401, "Token无效或已过期");
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<Map<String, String>> refreshToken(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(401, "Token格式错误");
        }

        String token = authorization.substring(7);
        String newToken = jwtUtil.refreshToken(token);

        if (newToken != null) {
            Map<String, String> data = new HashMap<>();
            data.put("token", newToken);
            return Result.success(data);
        }

        return Result.error(401, "Token刷新失败");
    }
}
