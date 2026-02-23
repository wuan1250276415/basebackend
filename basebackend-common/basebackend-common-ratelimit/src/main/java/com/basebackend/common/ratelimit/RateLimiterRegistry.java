package com.basebackend.common.ratelimit;

import java.util.EnumMap;
import java.util.Map;

/**
 * 限流器注册中心，管理不同算法的限流器实例
 * <p>
 * 根据 {@link RateLimitAlgorithm} 选择对应的 {@link RateLimiter} 实现，
 * 支持内存和 Redis 两种存储方式。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class RateLimiterRegistry {

    private final Map<RateLimitAlgorithm, RateLimiter> limiters;
    private final RateLimitAlgorithm defaultAlgorithm;

    public RateLimiterRegistry(RateLimitAlgorithm defaultAlgorithm,
                               Map<RateLimitAlgorithm, RateLimiter> limiters) {
        this.defaultAlgorithm = defaultAlgorithm;
        this.limiters = new EnumMap<>(limiters);
    }

    /**
     * 获取默认算法对应的限流器
     */
    public RateLimiter getDefaultLimiter() {
        return limiters.get(defaultAlgorithm);
    }

    /**
     * 根据算法获取限流器，DEFAULT 时使用默认算法
     *
     * @param algorithm 限流算法
     * @return 对应的限流器实现
     */
    public RateLimiter getLimiter(RateLimitAlgorithm algorithm) {
        if (algorithm == null || algorithm == RateLimitAlgorithm.DEFAULT) {
            return getDefaultLimiter();
        }
        RateLimiter limiter = limiters.get(algorithm);
        return limiter != null ? limiter : getDefaultLimiter();
    }

    /**
     * 从单个限流器创建注册中心（向后兼容）
     */
    public static RateLimiterRegistry ofSingle(RateLimiter rateLimiter) {
        Map<RateLimitAlgorithm, RateLimiter> map = new EnumMap<>(RateLimitAlgorithm.class);
        map.put(RateLimitAlgorithm.SLIDING_WINDOW, rateLimiter);
        map.put(RateLimitAlgorithm.TOKEN_BUCKET, rateLimiter);
        map.put(RateLimitAlgorithm.FIXED_WINDOW, rateLimiter);
        return new RateLimiterRegistry(RateLimitAlgorithm.SLIDING_WINDOW, map);
    }
}
