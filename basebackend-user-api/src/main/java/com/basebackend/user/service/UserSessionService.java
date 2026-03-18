package com.basebackend.user.service;

import java.util.List;
import java.util.Map;

/**
 * 用户登录会话服务
 */
public interface UserSessionService {

    /**
     * 替换用户当前会话，仅保留最新一组 access / refresh token。
     */
    void replaceSession(Long userId, String accessToken, String refreshToken,
                        long accessTokenTtlSeconds, long refreshTokenTtlSeconds);

    /**
     * 缓存用户权限与角色。
     */
    void storeAuthorities(Long userId, List<String> permissions, List<String> roles, long ttlSeconds);

    /**
     * 缓存在线用户信息。
     */
    void storeOnlineUser(Long userId, Map<String, Object> onlineUser, long ttlSeconds);

    /**
     * 获取当前 access token。
     */
    String getAccessToken(Long userId);

    /**
     * 获取当前 refresh token。
     */
    String getRefreshToken(Long userId);

    /**
     * 使用户当前会话失效。
     */
    void invalidateSession(Long userId);
}
