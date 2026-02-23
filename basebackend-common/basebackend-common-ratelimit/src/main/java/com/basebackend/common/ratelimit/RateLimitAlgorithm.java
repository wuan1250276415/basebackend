package com.basebackend.common.ratelimit;

/**
 * 限流算法枚举
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public enum RateLimitAlgorithm {

    /**
     * 使用配置文件中的默认算法
     */
    DEFAULT,

    /**
     * 滑动窗口算法
     */
    SLIDING_WINDOW,

    /**
     * 令牌桶算法
     */
    TOKEN_BUCKET,

    /**
     * 固定窗口算法
     */
    FIXED_WINDOW
}
