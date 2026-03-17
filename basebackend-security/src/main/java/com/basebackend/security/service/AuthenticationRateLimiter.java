package com.basebackend.security.service;

/**
 * 认证速率限制器接口
 * <p>
 * 基于滑动窗口算法限制认证尝试频率，防止暴力破解攻击。
 */
public interface AuthenticationRateLimiter {

    /**
     * 记录一次认证尝试
     *
     * @param key 限流键（如 IP 地址或用户名）
     * @return 如果未超过限流阈值返回 true，否则返回 false（请求应被拒绝）
     */
    boolean tryAcquire(String key);

    /**
     * 查询指定键是否处于封禁状态
     *
     * @param key 限流键
     * @return 是否被封禁
     */
    boolean isBlocked(String key);

    /**
     * 认证成功后重置计数器
     *
     * @param key 限流键
     */
    void resetAttempts(String key);

    /**
     * 获取剩余封禁秒数，未封禁返回 0
     *
     * @param key 限流键
     * @return 剩余封禁秒数
     */
    long getRemainingBlockSeconds(String key);
}
