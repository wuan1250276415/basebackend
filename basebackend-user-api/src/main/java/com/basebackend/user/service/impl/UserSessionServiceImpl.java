package com.basebackend.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.basebackend.cache.service.RedisService;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户登录会话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private static final String LOGIN_TOKEN_KEY = "login_tokens:";
    private static final String REFRESH_TOKEN_KEY = "refresh_tokens:";
    private static final String USER_PERMISSIONS_KEY = "user_permissions:";
    private static final String USER_ROLES_KEY = "user_roles:";
    private static final String ONLINE_USER_KEY = "online_users:";

    private final RedisService redisService;
    private final JwtUtil jwtUtil;

    @Override
    public void replaceSession(Long userId, String accessToken, String refreshToken,
                               long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {
        invalidateSession(userId);
        redisService.set(LOGIN_TOKEN_KEY + userId, accessToken, accessTokenTtlSeconds);
        redisService.set(REFRESH_TOKEN_KEY + userId, refreshToken, refreshTokenTtlSeconds);
    }

    @Override
    public void storeAuthorities(Long userId, List<String> permissions, List<String> roles, long ttlSeconds) {
        redisService.set(USER_PERMISSIONS_KEY + userId, permissions, ttlSeconds);
        redisService.set(USER_ROLES_KEY + userId, roles, ttlSeconds);
    }

    @Override
    public void storeOnlineUser(Long userId, Map<String, Object> onlineUser, long ttlSeconds) {
        redisService.set(ONLINE_USER_KEY + userId, onlineUser, ttlSeconds);
    }

    @Override
    public String getAccessToken(Long userId) {
        return readStringValue(LOGIN_TOKEN_KEY + userId);
    }

    @Override
    public String getRefreshToken(Long userId) {
        return readStringValue(REFRESH_TOKEN_KEY + userId);
    }

    @Override
    public void invalidateSession(Long userId) {
        if (userId == null) {
            return;
        }

        revokeTokenQuietly(getAccessToken(userId), userId, "access");
        revokeTokenQuietly(getRefreshToken(userId), userId, "refresh");

        redisService.delete(List.of(
                LOGIN_TOKEN_KEY + userId,
                REFRESH_TOKEN_KEY + userId,
                USER_PERMISSIONS_KEY + userId,
                USER_ROLES_KEY + userId,
                ONLINE_USER_KEY + userId
        ));
    }

    private String readStringValue(String key) {
        Object value = redisService.get(key);
        if (value == null) {
            return null;
        }
        String stringValue = String.valueOf(value);
        return StrUtil.isBlank(stringValue) ? null : stringValue;
    }

    private void revokeTokenQuietly(String token, Long userId, String tokenType) {
        if (StrUtil.isBlank(token)) {
            return;
        }

        try {
            jwtUtil.revokeToken(token);
        } catch (Exception e) {
            log.warn("吊销用户会话令牌失败: userId={}, tokenType={}, error={}", userId, tokenType, e.getMessage());
        }
    }
}
