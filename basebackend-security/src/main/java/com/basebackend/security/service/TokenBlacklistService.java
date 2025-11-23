package com.basebackend.security.service;

/**
 * Token 黑名单服务
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public interface TokenBlacklistService {

    /**
     * 将Token加入黑名单
     */
    void addToBlacklist(String token);

    /**
     * 检查Token是否在黑名单中
     */
    boolean isBlacklisted(String token);

    /**
     * 从黑名单中移除Token
     */
    void removeFromBlacklist(String token);

    /**
     * 添加用户会话
     */
    void addUserSession(String userId, String token);

    /**
     * 获取用户Token
     */
    String getUserToken(String userId);

    /**
     * 移除用户会话
     */
    void removeUserSession(String userId);

    /**
     * 强制用户下线
     */
    void forceLogoutUser(String userId);
}
