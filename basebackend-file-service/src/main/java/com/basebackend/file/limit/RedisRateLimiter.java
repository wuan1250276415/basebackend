package com.basebackend.file.limit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式限流器实现
 * <p>
 * 基于Redis的分布式限流器，适用于集群部署环境。
 * 使用Lua脚本保证原子性操作。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;

    /** 限流键前缀 */
    private static final String KEY_PREFIX = "rate_limit:";
    private static final String FAILURE_PREFIX = "password_failure:";
    private static final String COOLDOWN_PREFIX = "password_cooldown:";

    /**
     * 令牌桶Lua脚本
     * KEYS[1] = 令牌数键
     * KEYS[2] = 最后填充时间键
     * ARGV[1] = 容量
     * ARGV[2] = 填充速率（每秒令牌数）
     * ARGV[3] = 当前时间戳（毫秒）
     * 返回值: {是否允许, 剩余令牌数, 重置时间}
     */
    private static final String TOKEN_BUCKET_SCRIPT = """
            local tokens_key = KEYS[1]
            local last_time_key = KEYS[2]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            local tokens = tonumber(redis.call('GET', tokens_key) or capacity)
            local last_time = tonumber(redis.call('GET', last_time_key) or now)

            -- 计算并添加新令牌
            local elapsed = now - last_time
            local tokens_to_add = math.floor(elapsed * refill_rate / 1000)

            if tokens_to_add > 0 then
                tokens = math.min(capacity, tokens + tokens_to_add)
                redis.call('SET', last_time_key, now)
            end

            local allowed = 0
            if tokens > 0 then
                tokens = tokens - 1
                allowed = 1
            end

            redis.call('SET', tokens_key, tokens)
            redis.call('EXPIRE', tokens_key, 3600)
            redis.call('EXPIRE', last_time_key, 3600)

            local reset_time = now + math.floor(1000 / refill_rate)
            return {allowed, tokens, reset_time}
            """;

    /**
     * 固定窗口Lua脚本
     * KEYS[1] = 计数器键
     * ARGV[1] = 窗口大小（毫秒）
     * ARGV[2] = 最大请求数
     * 返回值: {是否允许, 剩余请求数, 重置时间}
     */
    private static final String FIXED_WINDOW_SCRIPT = """
            local counter_key = KEYS[1]
            local window_size = tonumber(ARGV[1])
            local max_requests = tonumber(ARGV[2])

            local current = tonumber(redis.call('GET', counter_key) or 0)
            local ttl = redis.call('TTL', counter_key)

            if ttl < 0 then
                ttl = math.floor(window_size / 1000)
            end

            local allowed = 0
            local remaining = max_requests - current - 1

            if current < max_requests then
                redis.call('INCR', counter_key)
                if ttl < 0 then
                    redis.call('EXPIRE', counter_key, math.floor(window_size / 1000))
                end
                allowed = 1
                remaining = max_requests - current - 1
            else
                remaining = 0
            end

            local reset_time = redis.call('TIME')[1] * 1000 + ttl * 1000
            return {allowed, remaining, reset_time}
            """;

    /**
     * 滑动窗口Lua脚本（基于有序集合）
     * KEYS[1] = 有序集合键
     * ARGV[1] = 窗口大小（毫秒）
     * ARGV[2] = 最大请求数
     * ARGV[3] = 当前时间戳（毫秒）
     * 返回值: {是否允许, 剩余请求数, 重置时间}
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
            local zset_key = KEYS[1]
            local window_size = tonumber(ARGV[1])
            local max_requests = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            local window_start = now - window_size

            -- 移除窗口外的请求
            redis.call('ZREMRANGEBYSCORE', zset_key, 0, window_start)

            -- 获取当前窗口内的请求数
            local current = redis.call('ZCARD', zset_key)

            local allowed = 0
            local remaining = max_requests - current - 1

            if current < max_requests then
                -- 添加当前请求
                redis.call('ZADD', zset_key, now, now .. ':' .. math.random(1000000))
                redis.call('EXPIRE', zset_key, math.ceil(window_size / 1000) + 1)
                allowed = 1
            else
                remaining = 0
            end

            local reset_time = now + window_size
            return {allowed, remaining, reset_time}
            """;

    private final DefaultRedisScript<List> tokenBucketScript = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT,
            List.class);
    private final DefaultRedisScript<List> fixedWindowScript = new DefaultRedisScript<>(FIXED_WINDOW_SCRIPT,
            List.class);
    private final DefaultRedisScript<List> slidingWindowScript = new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT,
            List.class);

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        if (!policy.isEnabled()) {
            return new RateLimitResult(true, Integer.MAX_VALUE, Integer.MAX_VALUE,
                    System.currentTimeMillis(), "限流已禁用");
        }

        if (!policy.isValid()) {
            throw new IllegalArgumentException("限流策略参数无效: " + policy);
        }

        try {
            return switch (policy.getLimitType()) {
                case TOKEN_BUCKET -> checkTokenBucket(key, policy);
                case FIXED_WINDOW -> checkFixedWindow(key, policy);
                case SLIDING_WINDOW -> checkSlidingWindow(key, policy);
                default -> throw new IllegalArgumentException("不支持的限流类型: " + policy.getLimitType());
            };
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis限流检查失败，降级为允许: key={}, error={}", key, e.getMessage());
            // Redis故障时降级为允许访问
            return new RateLimitResult(true, -1, -1, System.currentTimeMillis(), "限流服务降级");
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
        String failureKey = FAILURE_PREFIX + key;
        String cooldownKey = COOLDOWN_PREFIX + key;

        try {
            // 检查是否在冷却期
            String cooldownUntilStr = redisTemplate.opsForValue().get(cooldownKey);
            if (cooldownUntilStr != null) {
                long cooldownUntil = Long.parseLong(cooldownUntilStr);
                if (System.currentTimeMillis() < cooldownUntil) {
                    Long failureCount = redisTemplate.opsForValue().increment(failureKey, 0);
                    return new FailureResult(
                            failureCount != null ? failureCount.intValue() : 0,
                            cooldownUntil,
                            true,
                            String.format("密码错误次数过多，冷却至 %s", Instant.ofEpochMilli(cooldownUntil)));
                }
            }

            // 增加失败次数
            Long failureCount = redisTemplate.opsForValue().increment(failureKey);
            if (failureCount == 1) {
                // 第一次失败，设置过期时间
                redisTemplate.expire(failureKey, policy.getPasswordErrorCooldownMinutes() * 2, TimeUnit.MINUTES);
            }

            // 检查是否达到阈值
            if (failureCount >= policy.getPasswordErrorThreshold()) {
                long cooldownUntil = System.currentTimeMillis() +
                        TimeUnit.MINUTES.toMillis(policy.getPasswordErrorCooldownMinutes());
                redisTemplate.opsForValue().set(cooldownKey, String.valueOf(cooldownUntil),
                        policy.getPasswordErrorCooldownMinutes() + 1, TimeUnit.MINUTES);

                log.warn("密码错误次数达到阈值，启动冷却: key={}, failureCount={}", key, failureCount);

                return new FailureResult(
                        failureCount.intValue(),
                        cooldownUntil,
                        true,
                        String.format("密码错误次数过多，冷却 %d 分钟", policy.getPasswordErrorCooldownMinutes()));
            }

            return new FailureResult(
                    failureCount.intValue(),
                    0,
                    false,
                    String.format("密码错误 %d 次，最多允许 %d 次", failureCount, policy.getPasswordErrorThreshold()));
        } catch (Exception e) {
            log.error("记录密码失败次数异常: key={}, error={}", key, e.getMessage());
            return new FailureResult(0, 0, false, "记录失败");
        }
    }

    @Override
    public int getFailureCount(String key, RateLimitPolicy policy) {
        try {
            String value = redisTemplate.opsForValue().get(FAILURE_PREFIX + key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.error("获取失败次数异常: key={}, error={}", key, e.getMessage());
            return 0;
        }
    }

    @Override
    public void clearFailures(String key, RateLimitPolicy policy) {
        try {
            redisTemplate.delete(Arrays.asList(FAILURE_PREFIX + key, COOLDOWN_PREFIX + key));
            log.debug("清除密码失败记录: key={}", key);
        } catch (Exception e) {
            log.error("清除失败记录异常: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 令牌桶算法检查
     */
    private RateLimitResult checkTokenBucket(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        String tokensKey = KEY_PREFIX + "token:" + key + ":tokens";
        String timeKey = KEY_PREFIX + "token:" + key + ":time";

        List<Long> result = redisTemplate.execute(tokenBucketScript,
                Arrays.asList(tokensKey, timeKey),
                String.valueOf(policy.getBucketCapacity()),
                String.valueOf(policy.getRefillRate()),
                String.valueOf(System.currentTimeMillis()));

        if (result == null || result.size() < 3) {
            throw new RuntimeException("令牌桶脚本返回结果异常");
        }

        boolean allowed = result.get(0) == 1;
        int remainingTokens = result.get(1).intValue();
        long resetTime = result.get(2);

        if (allowed) {
            return new RateLimitResult(true, remainingTokens, 0, resetTime,
                    String.format("允许访问，剩余令牌: %d", remainingTokens));
        } else {
            RateLimitResult rateLimitResult = new RateLimitResult(false, 0, 0, resetTime,
                    String.format("请求过于频繁，请稍后重试"));
            log.warn("Redis令牌桶限流触发: key={}", key);
            throw new RateLimitExceededException("访问频率超限", rateLimitResult);
        }
    }

    /**
     * 固定窗口算法检查
     */
    private RateLimitResult checkFixedWindow(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        String counterKey = KEY_PREFIX + "window:" + key;
        long windowSizeMs = TimeUnit.valueOf(policy.getTimeUnit().name()).toMillis(policy.getWindowSize());

        List<Long> result = redisTemplate.execute(fixedWindowScript,
                List.of(counterKey),
                String.valueOf(windowSizeMs),
                String.valueOf(policy.getMaxRequests()));

        if (result == null || result.size() < 3) {
            throw new RuntimeException("固定窗口脚本返回结果异常");
        }

        boolean allowed = result.get(0) == 1;
        int remaining = result.get(1).intValue();
        long resetTime = result.get(2);

        if (allowed) {
            return new RateLimitResult(true, 0, remaining, resetTime,
                    String.format("允许访问，窗口剩余请求: %d", remaining));
        } else {
            RateLimitResult rateLimitResult = new RateLimitResult(false, 0, 0, resetTime,
                    "请求过于频繁，请稍后重试");
            log.warn("Redis固定窗口限流触发: key={}", key);
            throw new RateLimitExceededException("访问频率超限", rateLimitResult);
        }
    }

    /**
     * 滑动窗口算法检查
     */
    private RateLimitResult checkSlidingWindow(String key, RateLimitPolicy policy) throws RateLimitExceededException {
        String zsetKey = KEY_PREFIX + "sliding:" + key;
        long windowSizeMs = TimeUnit.valueOf(policy.getTimeUnit().name()).toMillis(policy.getWindowSize());

        List<Long> result = redisTemplate.execute(slidingWindowScript,
                List.of(zsetKey),
                String.valueOf(windowSizeMs),
                String.valueOf(policy.getMaxRequests()),
                String.valueOf(System.currentTimeMillis()));

        if (result == null || result.size() < 3) {
            throw new RuntimeException("滑动窗口脚本返回结果异常");
        }

        boolean allowed = result.get(0) == 1;
        int remaining = result.get(1).intValue();
        long resetTime = result.get(2);

        if (allowed) {
            return new RateLimitResult(true, 0, remaining, resetTime,
                    String.format("允许访问，窗口剩余请求: %d", remaining));
        } else {
            RateLimitResult rateLimitResult = new RateLimitResult(false, 0, 0, resetTime,
                    "请求过于频繁，请稍后重试");
            log.warn("Redis滑动窗口限流触发: key={}", key);
            throw new RateLimitExceededException("访问频率超限", rateLimitResult);
        }
    }
}
