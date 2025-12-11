package com.basebackend.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 认证速率限制器
 * <p>
 * 用于防止暴力破解攻击，限制认证失败次数。
 * 支持两种模式：
 * <ul>
 * <li>Redis模式：分布式环境，注入StringRedisTemplate</li>
 * <li>本地缓存模式：单机环境，不注入Redis时自动使用</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class AuthenticationRateLimiter {

    /**
     * Redis键前缀
     */
    private static final String REDIS_KEY_PREFIX = "auth:rate_limit:";

    /**
     * 默认最大尝试次数
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * 默认封禁时间（分钟）
     */
    private static final int DEFAULT_BLOCK_DURATION_MINUTES = 15;

    /**
     * Redis模板（可选）
     */
    @Nullable
    private final StringRedisTemplate redisTemplate;

    /**
     * 本地缓存：标识符 -> 失败尝试记录
     */
    private final ConcurrentHashMap<String, AttemptRecord> localCache = new ConcurrentHashMap<>();

    /**
     * 最大尝试次数
     */
    private final int maxAttempts;

    /**
     * 封禁时间（毫秒）
     */
    private final long blockDurationMillis;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板，为null时使用本地缓存模式
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public AuthenticationRateLimiter(@Nullable StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_MAX_ATTEMPTS, DEFAULT_BLOCK_DURATION_MINUTES);
    }

    /**
     * 构造函数（完整参数）
     *
     * @param redisTemplate        Redis模板
     * @param maxAttempts          最大尝试次数
     * @param blockDurationMinutes 封禁时间（分钟）
     */
    public AuthenticationRateLimiter(@Nullable StringRedisTemplate redisTemplate,
            int maxAttempts,
            int blockDurationMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.blockDurationMillis = TimeUnit.MINUTES.toMillis(blockDurationMinutes);

        if (redisTemplate != null) {
            log.info("AuthenticationRateLimiter initialized in Redis mode (maxAttempts={}, blockDuration={}min)",
                    maxAttempts, blockDurationMinutes);
        } else {
            log.info("AuthenticationRateLimiter initialized in local cache mode (maxAttempts={}, blockDuration={}min)",
                    maxAttempts, blockDurationMinutes);
        }
    }

    /**
     * 检查标识符是否被允许进行认证
     *
     * @param identifier 标识符（用户名、邮箱或IP地址）
     * @return 是否允许
     */
    public boolean isAllowed(String identifier) {
        // 空标识符始终允许
        if (!StringUtils.hasText(identifier)) {
            return true;
        }

        if (redisTemplate != null) {
            return isAllowedRedis(identifier);
        } else {
            return isAllowedLocal(identifier);
        }
    }

    /**
     * 记录失败尝试
     *
     * @param identifier 标识符
     */
    public void recordFailedAttempt(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return;
        }

        if (redisTemplate != null) {
            recordFailedAttemptRedis(identifier);
        } else {
            recordFailedAttemptLocal(identifier);
        }
    }

    /**
     * 清除失败记录（登录成功后调用）
     *
     * @param identifier 标识符
     */
    public void clearFailedAttempts(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return;
        }

        if (redisTemplate != null) {
            clearFailedAttemptsRedis(identifier);
        } else {
            clearFailedAttemptsLocal(identifier);
        }
    }

    /**
     * 获取剩余封禁时间（秒）
     *
     * @param identifier 标识符
     * @return 剩余秒数，未封禁返回0
     */
    public long getRemainingBlockTime(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return 0;
        }

        if (redisTemplate != null) {
            return getRemainingBlockTimeRedis(identifier);
        } else {
            return getRemainingBlockTimeLocal(identifier);
        }
    }

    // ==================== Redis模式实现 ====================

    private boolean isAllowedRedis(String identifier) {
        try {
            String key = REDIS_KEY_PREFIX + identifier;
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return true;
            }

            int attempts = Integer.parseInt(value);
            return attempts < maxAttempts;
        } catch (Exception e) {
            log.warn("Redis rate limit check failed for '{}', allowing by default: {}", identifier, e.getMessage());
            return true;
        }
    }

    private void recordFailedAttemptRedis(String identifier) {
        try {
            String key = REDIS_KEY_PREFIX + identifier;
            Long newCount = redisTemplate.opsForValue().increment(key);

            if (newCount != null && newCount == 1) {
                // 第一次记录，设置过期时间
                redisTemplate.expire(key, blockDurationMillis, TimeUnit.MILLISECONDS);
            }

            log.debug("Recorded failed attempt for '{}': count={}", identifier, newCount);
        } catch (Exception e) {
            log.warn("Failed to record failed attempt in Redis for '{}': {}", identifier, e.getMessage());
        }
    }

    private void clearFailedAttemptsRedis(String identifier) {
        try {
            String key = REDIS_KEY_PREFIX + identifier;
            redisTemplate.delete(key);
            log.debug("Cleared failed attempts for '{}'", identifier);
        } catch (Exception e) {
            log.warn("Failed to clear failed attempts in Redis for '{}': {}", identifier, e.getMessage());
        }
    }

    private long getRemainingBlockTimeRedis(String identifier) {
        try {
            String key = REDIS_KEY_PREFIX + identifier;
            String value = redisTemplate.opsForValue().get(key);

            if (value == null || Integer.parseInt(value) < maxAttempts) {
                return 0;
            }

            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.warn("Failed to get remaining block time from Redis for '{}': {}", identifier, e.getMessage());
            return 0;
        }
    }

    // ==================== 本地缓存模式实现 ====================

    private boolean isAllowedLocal(String identifier) {
        AttemptRecord record = localCache.get(identifier);

        if (record == null) {
            return true;
        }

        // 检查是否过期
        if (record.isExpired()) {
            localCache.remove(identifier);
            return true;
        }

        return record.getAttempts() < maxAttempts;
    }

    private void recordFailedAttemptLocal(String identifier) {
        localCache.compute(identifier, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                // 新记录或已过期，创建新记录
                return new AttemptRecord(1, System.currentTimeMillis() + blockDurationMillis);
            } else {
                // 增加计数
                existing.incrementAttempts();
                return existing;
            }
        });

        log.debug("Recorded failed attempt for '{}' (local)", identifier);
    }

    private void clearFailedAttemptsLocal(String identifier) {
        localCache.remove(identifier);
        log.debug("Cleared failed attempts for '{}' (local)", identifier);
    }

    private long getRemainingBlockTimeLocal(String identifier) {
        AttemptRecord record = localCache.get(identifier);

        if (record == null || record.isExpired() || record.getAttempts() < maxAttempts) {
            return 0;
        }

        long remaining = record.getExpirationTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    /**
     * 尝试记录（用于本地缓存模式）
     */
    private static class AttemptRecord {
        private int attempts;
        private final long expirationTime;

        AttemptRecord(int attempts, long expirationTime) {
            this.attempts = attempts;
            this.expirationTime = expirationTime;
        }

        int getAttempts() {
            return attempts;
        }

        void incrementAttempts() {
            this.attempts++;
        }

        long getExpirationTime() {
            return expirationTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
