package com.basebackend.security.service.impl;

import com.basebackend.security.config.SecurityBaselineProperties;
import com.basebackend.security.service.AuthenticationRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 滑动窗口的认证速率限制器实现
 * <p>
 * 策略：在 {@code windowDuration} 时间窗口内允许最多 {@code maxAttempts} 次尝试，
 * 超过后封禁 {@code blockDuration} 时间。认证成功后重置计数。
 */
@Slf4j
public class RedisAuthenticationRateLimiter implements AuthenticationRateLimiter {

    private static final String ATTEMPT_KEY_PREFIX = "auth:ratelimit:attempt:";
    private static final String BLOCK_KEY_PREFIX = "auth:ratelimit:block:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final int maxAttempts;
    private final Duration blockDuration;
    private final Duration windowDuration;

    public RedisAuthenticationRateLimiter(RedisTemplate<String, Object> redisTemplate,
                                          SecurityBaselineProperties properties) {
        this.redisTemplate = redisTemplate;
        SecurityBaselineProperties.RateLimitConfig config = properties.getRateLimit();
        this.maxAttempts = config.getMaxAttempts();
        this.blockDuration = config.getBlockDuration();
        this.windowDuration = config.getWindowDuration();
    }

    @Override
    public boolean tryAcquire(String key) {
        try {
            // 先检查是否已被封禁
            if (isBlocked(key)) {
                log.warn("认证请求被速率限制拒绝（封禁中）: key={}", key);
                return false;
            }

            String attemptKey = ATTEMPT_KEY_PREFIX + key;
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();

            // 原子递增计数
            Long attempts = ops.increment(attemptKey);
            if (attempts == null) {
                return true;
            }

            // 首次写入时设置窗口过期时间
            if (attempts == 1) {
                redisTemplate.expire(attemptKey, windowDuration.toSeconds(), TimeUnit.SECONDS);
            }

            // 超过阈值则封禁
            if (attempts > maxAttempts) {
                String blockKey = BLOCK_KEY_PREFIX + key;
                ops.set(blockKey, "1", blockDuration.toSeconds(), TimeUnit.SECONDS);
                // 删除计数器，封禁期结束后重新计数
                redisTemplate.delete(attemptKey);
                log.warn("认证尝试超过阈值，已封禁: key={}, attempts={}, blockDuration={}s",
                        key, attempts, blockDuration.toSeconds());
                return false;
            }

            return true;
        } catch (Exception e) {
            // Redis 不可用时放行，避免速率限制故障导致全部用户无法登录
            log.error("速率限制检查失败（Redis 不可用），放行请求: key={}", key, e);
            return true;
        }
    }

    @Override
    public boolean isBlocked(String key) {
        try {
            String blockKey = BLOCK_KEY_PREFIX + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
        } catch (Exception e) {
            log.error("检查封禁状态失败: key={}", key, e);
            return false;
        }
    }

    @Override
    public void resetAttempts(String key) {
        try {
            String attemptKey = ATTEMPT_KEY_PREFIX + key;
            redisTemplate.delete(attemptKey);
            log.debug("认证成功，已重置速率限制计数器: key={}", key);
        } catch (Exception e) {
            log.error("重置速率限制计数器失败: key={}", key, e);
        }
    }

    @Override
    public long getRemainingBlockSeconds(String key) {
        try {
            String blockKey = BLOCK_KEY_PREFIX + key;
            Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.error("查询剩余封禁时间失败: key={}", key, e);
            return 0;
        }
    }
}
