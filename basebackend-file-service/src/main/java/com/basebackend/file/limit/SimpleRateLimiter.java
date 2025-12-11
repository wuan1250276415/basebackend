package com.basebackend.file.limit;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单限流器实现（内存版）
 *
 * 基于 ConcurrentHashMap 的内存实现，适用于单机部署
 * 生产环境建议使用 Redis 分布式限流器
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
public class SimpleRateLimiter implements RateLimiter {

    /**
     * 令牌桶状态存储（key -> TokenBucketState）
     */
    private final ConcurrentHashMap<String, TokenBucketState> tokenBuckets = new ConcurrentHashMap<>();

    /**
     * 固定窗口状态存储（key -> FixedWindowState）
     */
    private final ConcurrentHashMap<String, FixedWindowState> fixedWindows = new ConcurrentHashMap<>();

    /**
     * 密码失败记录存储（key -> PasswordFailureState）
     */
    private final ConcurrentHashMap<String, PasswordFailureState> passwordFailures = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        if (!policy.isEnabled()) {
            return new RateLimitResult(true, Integer.MAX_VALUE, Integer.MAX_VALUE,
                    System.currentTimeMillis(), "限流已禁用");
        }

        switch (policy.getLimitType()) {
            case TOKEN_BUCKET:
                return checkTokenBucket(key, policy);
            case FIXED_WINDOW:
                return checkFixedWindow(key, policy);
            case SLIDING_WINDOW:
                // TODO: 实现滑动窗口算法
                throw new UnsupportedOperationException("滑动窗口限流算法待实现");
            default:
                throw new IllegalArgumentException("不支持的限流类型: " + policy.getLimitType());
        }
    }

    @Override
    public boolean isAllowed(String key, RateLimitPolicy policy) {
        try {
            RateLimitResult result = check(key, policy);
            return result.isAllowed();
        } catch (RateLimitExceededException e) {
            return false;
        }
    }

    @Override
    public FailureResult recordFailure(String key, RateLimitPolicy policy) {
        if (!policy.getLimitType().equals(RateLimitPolicy.LimitType.PASSWORD_COOLDOWN)) {
            throw new IllegalArgumentException("recordFailure 仅适用于密码冷却策略");
        }

        PasswordFailureState state = passwordFailures.computeIfAbsent(key, k -> new PasswordFailureState());
        long now = System.currentTimeMillis();

        // 检查是否在冷却期内
        if (now < state.cooldownUntil) {
            return new FailureResult(
                    state.failureCount.get(),
                    state.cooldownUntil,
                    true,
                    String.format("密码错误次数过多，冷却至 %s",
                            Instant.ofEpochMilli(state.cooldownUntil))
            );
        }

        // 增加失败次数
        state.failureCount.incrementAndGet();

        // 检查是否达到冷却阈值
        if (state.failureCount.get() >= policy.getPasswordErrorThreshold()) {
            state.cooldownUntil = now + TimeUnit.MINUTES.toMillis(policy.getPasswordErrorCooldownMinutes());
            log.warn("密码错误次数达到阈值，启动冷却: key={}, failureCount={}, cooldownUntil={}",
                    key, state.failureCount.get(), Instant.ofEpochMilli(state.cooldownUntil));
            return new FailureResult(
                    state.failureCount.get(),
                    state.cooldownUntil,
                    true,
                    String.format("密码错误次数过多，冷却 %d 分钟",
                            policy.getPasswordErrorCooldownMinutes())
            );
        }

        return new FailureResult(
                state.failureCount.get(),
                0,
                false,
                String.format("密码错误 %d 次，最多允许 %d 次",
                        state.failureCount.get(), policy.getPasswordErrorThreshold())
        );
    }

    @Override
    public int getFailureCount(String key, RateLimitPolicy policy) {
        PasswordFailureState state = passwordFailures.get(key);
        return state != null ? state.failureCount.get() : 0;
    }

    @Override
    public void clearFailures(String key, RateLimitPolicy policy) {
        passwordFailures.remove(key);
        log.debug("清除密码失败记录: key={}", key);
    }

    /**
     * 令牌桶算法检查（修复版：拒绝时抛出异常）
     */
    private RateLimitResult checkTokenBucket(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        // 验证策略参数
        if (!policy.isValid()) {
            throw new IllegalArgumentException("限流策略参数无效: " + policy);
        }

        long now = System.currentTimeMillis();
        TokenBucketState state = tokenBuckets.computeIfAbsent(key, k -> new TokenBucketState(policy.getBucketCapacity()));

        // 同步更新令牌数
        synchronized (state) {
            // 计算需要补充的令牌数
            long elapsedMs = now - state.lastRefillTime;
            long tokensToAdd = elapsedMs * policy.getRefillRate() / 1000;

            if (tokensToAdd > 0) {
                state.tokens = Math.min(policy.getBucketCapacity(), state.tokens + (int) tokensToAdd);
                state.lastRefillTime = now;
            }

            // 检查是否有可用令牌
            if (state.tokens > 0) {
                state.tokens--;
                int remaining = state.tokens;
                long resetTime = now + (1000 / policy.getRefillRate()); // 下一个令牌补充时间

                return new RateLimitResult(
                        true,
                        remaining,
                        0,
                        resetTime,
                        String.format("允许访问，剩余令牌: %d", remaining)
                );
            } else {
                // 计算需要等待的时间
                long waitTime = (1000 / policy.getRefillRate());
                long resetTime = now + waitTime;

                RateLimitResult result = new RateLimitResult(
                        false,
                        0,
                        0,
                        resetTime,
                        String.format("请求过于频繁，请等待 %d 毫秒", waitTime)
                );

                log.warn("令牌桶限流触发: key={}, capacity={}, refillRate={}, waitTime={}ms",
                        key, policy.getBucketCapacity(), policy.getRefillRate(), waitTime);

                // 🔥 关键修复：拒绝时抛出异常而不是返回false结果
                throw new RateLimitExceededException("访问频率超限", result);
            }
        }
    }

    /**
     * 固定窗口算法检查（修复版：拒绝时抛出异常）
     */
    private RateLimitResult checkFixedWindow(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        // 验证策略参数
        if (!policy.isValid()) {
            throw new IllegalArgumentException("限流策略参数无效: " + policy);
        }

        long now = System.currentTimeMillis();
        long windowSizeMs = TimeUnit.valueOf(policy.getTimeUnit().name()).toMillis(policy.getWindowSize());

        FixedWindowState state = fixedWindows.computeIfAbsent(key, k -> new FixedWindowState());

        // 检查是否需要重置窗口
        if (now - state.windowStart >= windowSizeMs) {
            synchronized (state) {
                if (now - state.windowStart >= windowSizeMs) {
                    state.requestCount.set(0);
                    state.windowStart = now;
                }
            }
        }

        // 检查是否超出限制
        int currentCount = state.requestCount.get();
        if (currentCount < policy.getMaxRequests()) {
            state.requestCount.incrementAndGet();
            int remainingRequests = policy.getMaxRequests() - currentCount - 1;
            long resetTime = state.windowStart + windowSizeMs;

            return new RateLimitResult(
                    true,
                    0,
                    remainingRequests,
                    resetTime,
                    String.format("允许访问，窗口剩余请求: %d", remainingRequests)
            );
        } else {
            long resetTime = state.windowStart + windowSizeMs;

            RateLimitResult result = new RateLimitResult(
                    false,
                    0,
                    0,
                    resetTime,
                    String.format("请求过于频繁，请等待 %d 秒后重试",
                            TimeUnit.MILLISECONDS.toSeconds(resetTime - now))
            );

            log.warn("固定窗口限流触发: key={}, windowSize={}, maxRequests={}, resetTime={}",
                    key, policy.getWindowSize(), policy.getMaxRequests(), Instant.ofEpochMilli(resetTime));

            // 🔥 关键修复：拒绝时抛出异常
            throw new RateLimitExceededException("访问频率超限", result);
        }
    }

    /**
     * 令牌桶状态
     */
    private static class TokenBucketState {
        int tokens; // 初始化为满容量，避免首个请求被拒
        long lastRefillTime;

        TokenBucketState(int capacity) {
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }
    }

    /**
     * 固定窗口状态
     */
    private static class FixedWindowState {
        AtomicInteger requestCount = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }

    /**
     * 密码失败状态
     */
    private static class PasswordFailureState {
        AtomicInteger failureCount = new AtomicInteger(0);
        long cooldownUntil = 0;
    }
}
