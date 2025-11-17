package com.basebackend.oauth2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2.0用户信息端点
 * 提供OpenID Connect用户信息接口
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@Tag(name = "OAuth2.0用户信息", description = "OAuth2.0和OpenID Connect用户信息相关接口")
@RequiredArgsConstructor
public class UserInfoController {

    /**
     * 获取当前用户信息
     */
    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取当前用户信息", description = "返回OpenID Connect格式的用户信息")
    public ResponseEntity<?> userinfo(Authentication authentication) {
        log.debug("OAuth2.0获取用户信息");

        try {
            // 解析JWT令牌中的用户信息
            Map<String, Object> userInfo = new HashMap<>();

            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                Map<String, Object> claims = (Map<String, Object>) jwtToken.getToken().getClaims();

                userInfo.put("sub", claims.get("sub")); // 用户唯一标识
                userInfo.put("userId", claims.get("userId"));
                userInfo.put("username", claims.get("username"));
                userInfo.put("nickname", claims.get("nickname"));
                userInfo.put("email", claims.get("email"));
                userInfo.put("email_verified", claims.get("email_verified"));
                userInfo.put("phone", claims.get("phone"));
                userInfo.put("phone_verified", claims.get("phone_verified"));
                userInfo.put("avatar", claims.get("avatar"));
                userInfo.put("gender", claims.get("gender"));
                userInfo.put("deptId", claims.get("deptId"));
                userInfo.put("deptName", claims.get("deptName"));
                userInfo.put("userType", claims.get("userType"));
                userInfo.put("roles", claims.get("roles"));
                userInfo.put("permissions", claims.get("permissions"));
            } else {
                // 从Authentication中获取用户信息
                Object principal = authentication.getPrincipal();
                if (principal instanceof com.basebackend.oauth2.user.OAuth2UserDetails) {
                    com.basebackend.oauth2.user.OAuth2UserDetails userDetails =
                            (com.basebackend.oauth2.user.OAuth2UserDetails) principal;

                    userInfo.put("sub", userDetails.getUserId().toString());
                    userInfo.put("userId", userDetails.getUserId());
                    userInfo.put("username", userDetails.getUsername());
                    userInfo.put("nickname", userDetails.getNickname());
                    userInfo.put("email", userDetails.getEmail());
                    userInfo.put("phone", userDetails.getPhone());
                    userInfo.put("avatar", userDetails.getAvatar());
                    userInfo.put("gender", userDetails.getGender());
                    userInfo.put("deptId", userDetails.getDeptId());
                    userInfo.put("deptName", userDetails.getDeptName());
                    userInfo.put("userType", userDetails.getUserType());
                    userInfo.put("roles", userDetails.getRoles());
                    userInfo.put("permissions", userDetails.getPermissions());
                }
            }

            // 添加OpenID Connect标准字段
            userInfo.put("iss", "http://localhost:8082"); // 发行者
            userInfo.put("aud", "basebackend-web"); // 受众

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_request", "error_description", "获取用户信息失败"));
        }
    }

    /**
     * 检查令牌有效性
     */
    @GetMapping("/check_token")
    @Operation(summary = "检查令牌有效性", description = "验证当前访问令牌是否有效")
    public ResponseEntity<?> checkToken(Authentication authentication) {
        log.debug("OAuth2.0检查令牌有效性");

        Map<String, Object> result = new HashMap<>();
        result.put("active", true);
        result.put("username", authentication.getName());
        result.put("scope", "read write user_info");
        result.put("client_id", "basebackend-web");

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            result.put("exp", jwtToken.getToken().getExpiresAt());
            result.put("iat", jwtToken.getToken().getIssuedAt());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取令牌信息
     */
    @GetMapping("/token_info")
    @Operation(summary = "获取令牌信息", description = "返回当前访问令牌的详细信息")
    public ResponseEntity<?> tokenInfo(Authentication authentication) {
        log.debug("OAuth2.0获取令牌信息");

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("principal", authentication.getName());
        tokenInfo.put("authorities", authentication.getAuthorities());

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            tokenInfo.put("token", jwtToken.getToken().getTokenValue());
            tokenInfo.put("claims", jwtToken.getToken().getClaims());
            tokenInfo.put("expiresAt", jwtToken.getToken().getExpiresAt());
            tokenInfo.put("issuedAt", jwtToken.getToken().getIssuedAt());
        }

        return ResponseEntity.ok(tokenInfo);
    }
}
