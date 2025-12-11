package com.basebackend.file.limit;

/**
 * 限流器接口
 *
 * 定义不同限流算法的统一规范
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
public interface RateLimiter {

    /**
     * 检查是否允许访问
     *
     * @param key 限流键（通常是 userId + shareCode 或 IP）
     * @param policy 限流策略
     * @return 限流检查结果
     * @throws RateLimitExceededException 当触发限流时抛出异常
     */
    RateLimitResult check(String key, RateLimitPolicy policy) throws RateLimitExceededException;

    /**
     * 检查是否允许访问（不抛出异常）
     *
     * @param key 限流键
     * @param policy 限流策略
     * @return 如果允许返回 true，否则返回 false
     */
    boolean isAllowed(String key, RateLimitPolicy policy);

    /**
     * 记录失败事件（用于密码错误冷却等）
     *
     * @param key 限流键
     * @param policy 限流策略
     * @return 失败次数和剩余冷却时间
     */
    FailureResult recordFailure(String key, RateLimitPolicy policy);

    /**
     * 获取失败次数
     *
     * @param key 限流键
     * @param policy 限流策略
     * @return 失败次数
     */
    int getFailureCount(String key, RateLimitPolicy policy);

    /**
     * 清除失败记录（通常在成功后）
     *
     * @param key 限流键
     * @param policy 限流策略
     */
    void clearFailures(String key, RateLimitPolicy policy);

    /**
     * 限流检查结果
     */
    class RateLimitResult {
        private final boolean allowed;
        private final int remainingTokens; // 剩余令牌数（令牌桶算法）
        private final int windowRemaining; // 窗口剩余请求数（窗口算法）
        private final long resetTime; // 重置时间戳（毫秒）
        private final String message;

        public RateLimitResult(boolean allowed, int remainingTokens, int windowRemaining,
                             long resetTime, String message) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.windowRemaining = windowRemaining;
            this.resetTime = resetTime;
            this.message = message;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public int getRemainingTokens() {
            return remainingTokens;
        }

        public int getWindowRemaining() {
            return windowRemaining;
        }

        public long getResetTime() {
            return resetTime;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 失败结果（密码错误冷却等）
     */
    class FailureResult {
        private final int failureCount;
        private final long cooldownUntil; // 冷却到期时间戳（毫秒）
        private final boolean inCooldown;
        private final String message;

        public FailureResult(int failureCount, long cooldownUntil, boolean inCooldown, String message) {
            this.failureCount = failureCount;
            this.cooldownUntil = cooldownUntil;
            this.inCooldown = inCooldown;
            this.message = message;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public long getCooldownUntil() {
            return cooldownUntil;
        }

        public boolean isInCooldown() {
            return inCooldown;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 限流异常
     */
    static class RateLimitExceededException extends Exception {
        private final RateLimitResult result;

        public RateLimitExceededException(String message, RateLimitResult result) {
            super(message);
            this.result = result;
        }

        public RateLimitExceededException(String message) {
            super(message);
            this.result = null;
        }

        public RateLimitResult getResult() {
            return result;
        }
    }
}
