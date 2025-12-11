package com.basebackend.file.limit;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * 限流策略配置
 *
 * 定义访问限速和密码错误限制的参数
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Data
public class RateLimitPolicy {

    /**
     * 限流类型
     */
    private LimitType limitType;

    /**
     * 令牌桶容量（最大突发请求数）
     */
    private int bucketCapacity;

    /**
     * 令牌补充速率（每秒补充的令牌数）
     */
    private int refillRate;

    /**
     * 固定窗口大小
     */
    private int windowSize;

    /**
     * 固定窗口内最大请求数
     */
    private int maxRequests;

    /**
     * 窗口时间单位
     */
    private TimeUnit timeUnit;

    /**
     * 密码错误次数阈值
     */
    private int passwordErrorThreshold;

    /**
     * 密码错误冷却时间
     */
    private int passwordErrorCooldownMinutes;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 限流类型枚举
     */
    public enum LimitType {
        /**
         * 令牌桶算法（平滑突发流量）
         */
        TOKEN_BUCKET,

        /**
         * 固定窗口算法（固定时间窗口内限制请求数）
         */
        FIXED_WINDOW,

        /**
         * 滑动窗口算法（更精确的限流控制）
         */
        SLIDING_WINDOW,

        /**
         * 密码错误冷却（特殊限流）
         */
        PASSWORD_COOLDOWN
    }

    /**
     * 创建默认的访问限流策略
     */
    public static RateLimitPolicy defaultAccessLimit() {
        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setLimitType(LimitType.TOKEN_BUCKET);
        policy.setBucketCapacity(10); // 最多允许 10 次突发
        policy.setRefillRate(5); // 每秒补充 5 个令牌
        policy.setEnabled(true);
        return policy;
    }

    /**
     * 创建固定窗口限流策略
     */
    public static RateLimitPolicy fixedWindowLimit() {
        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setLimitType(LimitType.FIXED_WINDOW);
        policy.setWindowSize(60); // 60秒窗口
        policy.setMaxRequests(60); // 最多60次请求
        policy.setTimeUnit(TimeUnit.SECONDS);
        policy.setEnabled(true);
        return policy;
    }

    /**
     * 创建密码错误冷却策略
     */
    public static RateLimitPolicy passwordCooldown() {
        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setLimitType(LimitType.PASSWORD_COOLDOWN);
        policy.setPasswordErrorThreshold(5); // 5次失败
        policy.setPasswordErrorCooldownMinutes(15); // 冷却15分钟
        policy.setEnabled(true);
        return policy;
    }

    /**
     * 创建滑动窗口限流策略
     */
    public static RateLimitPolicy slidingWindowLimit() {
        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setLimitType(LimitType.SLIDING_WINDOW);
        policy.setWindowSize(60); // 60秒窗口
        policy.setMaxRequests(100); // 最多100次请求
        policy.setTimeUnit(TimeUnit.SECONDS);
        policy.setEnabled(true);
        return policy;
    }

    /**
     * 创建自定义滑动窗口限流策略
     *
     * @param windowSize  窗口大小
     * @param maxRequests 最大请求数
     * @param timeUnit    时间单位
     */
    public static RateLimitPolicy slidingWindowLimit(int windowSize, int maxRequests, TimeUnit timeUnit) {
        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setLimitType(LimitType.SLIDING_WINDOW);
        policy.setWindowSize(windowSize);
        policy.setMaxRequests(maxRequests);
        policy.setTimeUnit(timeUnit);
        policy.setEnabled(true);
        return policy;
    }

    /**
     * 验证策略参数是否有效
     */
    public boolean isValid() {
        if (!enabled) {
            return true; // 禁用状态认为有效
        }

        return switch (limitType) {
            case TOKEN_BUCKET -> bucketCapacity > 0 && refillRate > 0;
            case FIXED_WINDOW, SLIDING_WINDOW -> windowSize > 0 && maxRequests > 0 && timeUnit != null;
            case PASSWORD_COOLDOWN -> passwordErrorThreshold > 0 && passwordErrorCooldownMinutes > 0;
        };
    }
}
