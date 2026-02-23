package com.basebackend.common.ratelimit.impl;

import com.basebackend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;

/**
 * Redis 滑动窗口限流器
 * <p>
 * 基于 Redis Sorted Set（ZSET）实现精确滑动窗口。
 * Lua 脚本保证操作原子性：
 * <ol>
 *   <li>ZREMRANGEBYSCORE 删除窗口外的记录</li>
 *   <li>ZCARD 获取当前窗口内请求数</li>
 *   <li>如果 &lt; limit，ZADD 当前时间戳，EXPIRE 设置 TTL</li>
 *   <li>返回是否允许</li>
 * </ol>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class RedisSlidingWindowRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisSlidingWindowRateLimiter.class);

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local window_start = tonumber(ARGV[1])
            local now = ARGV[2]
            local limit = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            local unique_member = ARGV[5]
            redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
            local count = redis.call('ZCARD', key)
            if count < limit then
                redis.call('ZADD', key, now, unique_member)
                redis.call('EXPIRE', key, ttl)
                return 1
            end
            return 0
            """;

    private static final DefaultRedisScript<Long> REDIS_SCRIPT;

    static {
        REDIS_SCRIPT = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisSlidingWindowRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;
        String uniqueMember = now + ":" + UUID.randomUUID().toString().substring(0, 8);

        String rateLimitKey = "ratelimit:sw:" + key;

        try {
            Long result = redisTemplate.execute(
                    REDIS_SCRIPT,
                    Collections.singletonList(rateLimitKey),
                    String.valueOf(windowStart),
                    String.valueOf(now),
                    String.valueOf(limit),
                    String.valueOf(windowSeconds + 1),
                    uniqueMember
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis sliding window rate limiter failed for key={}, allowing request as fallback", key, e);
            return true;
        }
    }
}
