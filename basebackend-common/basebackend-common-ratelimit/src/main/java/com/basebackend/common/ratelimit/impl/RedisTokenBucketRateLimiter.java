package com.basebackend.common.ratelimit.impl;

import com.basebackend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;

/**
 * Redis 令牌桶限流器
 * <p>
 * 基于 Redis 的分布式令牌桶实现。
 * 使用两个 Redis key 维护桶状态：
 * <ul>
 *   <li>{key}:tokens — 当前令牌数</li>
 *   <li>{key}:timestamp — 上次填充时间</li>
 * </ul>
 * Lua 脚本保证操作原子性。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class RedisTokenBucketRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenBucketRateLimiter.class);

    private static final String LUA_SCRIPT = """
            local tokens_key = KEYS[1]
            local timestamp_key = KEYS[2]
            local capacity = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            local tokens = tonumber(redis.call('GET', tokens_key))
            local last_time = tonumber(redis.call('GET', timestamp_key))
            if tokens == nil then
                tokens = capacity
                last_time = now
            end
            local elapsed = now - last_time
            local new_tokens = math.min(capacity, tokens + elapsed * rate)
            if new_tokens >= 1 then
                new_tokens = new_tokens - 1
                redis.call('SET', tokens_key, tostring(new_tokens), 'EX', ttl)
                redis.call('SET', timestamp_key, tostring(now), 'EX', ttl)
                return 1
            end
            redis.call('SET', tokens_key, tostring(new_tokens), 'EX', ttl)
            redis.call('SET', timestamp_key, tostring(now), 'EX', ttl)
            return 0
            """;

    private static final DefaultRedisScript<Long> REDIS_SCRIPT;

    static {
        REDIS_SCRIPT = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBucketRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        String tokensKey = "ratelimit:tb:" + key + ":tokens";
        String timestampKey = "ratelimit:tb:" + key + ":timestamp";
        double rate = (double) limit / windowSeconds;
        long nowSeconds = System.currentTimeMillis() / 1000;

        try {
            Long result = redisTemplate.execute(
                    REDIS_SCRIPT,
                    Arrays.asList(tokensKey, timestampKey),
                    String.valueOf(limit),
                    String.valueOf(rate),
                    String.valueOf(nowSeconds),
                    String.valueOf(windowSeconds * 2)
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis token bucket rate limiter failed for key={}, allowing request as fallback", key, e);
            return true;
        }
    }
}
