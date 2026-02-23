package com.basebackend.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 黑名单/吊销管理
 * <p>
 * 优先使用 Redis（key: jwt:blacklist:{jti}，TTL = Token 剩余有效期）。
 * 当 Redis 不可用时降级为内存 ConcurrentHashMap + 定时清理。
 */
@Slf4j
public class JwtTokenBlacklist {

    private static final String REDIS_KEY_PREFIX = "jwt:blacklist:";
    private static final String BLACKLIST_VALUE = "1";

    @Nullable
    private final StringRedisTemplate redisTemplate;

    /** 降级用内存黑名单：jti -> 过期时间戳 */
    private final Map<String, Long> memoryBlacklist = new ConcurrentHashMap<>();
    private volatile ScheduledExecutorService cleanupExecutor;

    public JwtTokenBlacklist(@Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (!isRedisAvailable()) {
            startMemoryCleanup();
        }
    }

    /**
     * 吊销 Token
     *
     * @param jti       JWT ID
     * @param expiresAt Token 过期时间戳（毫秒）
     */
    public void revoke(String jti, long expiresAt) {
        long ttlMillis = expiresAt - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            // Token 已经过期，无需加入黑名单
            return;
        }

        if (tryRedisRevoke(jti, ttlMillis)) {
            return;
        }

        // 降级到内存
        ensureMemoryCleanupStarted();
        memoryBlacklist.put(jti, expiresAt);
        log.debug("Token {} added to in-memory blacklist, expires at {}", jti, expiresAt);
    }

    /**
     * 检查 Token 是否已被吊销
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        // 先查 Redis
        if (tryRedisCheck(jti)) {
            return true;
        }

        // 再查内存
        Long expiresAt = memoryBlacklist.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            memoryBlacklist.remove(jti);
            return false;
        }
        return true;
    }

    private boolean tryRedisRevoke(String jti, long ttlMillis) {
        if (!isRedisAvailable()) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + jti,
                    BLACKLIST_VALUE,
                    Duration.ofMillis(ttlMillis));
            log.debug("Token {} added to Redis blacklist, TTL {}ms", jti, ttlMillis);
            return true;
        } catch (Exception e) {
            log.warn("Redis blacklist write failed, falling back to memory: {}", e.getMessage());
            return false;
        }
    }

    private boolean tryRedisCheck(String jti) {
        if (!isRedisAvailable()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("Redis blacklist check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    private void ensureMemoryCleanupStarted() {
        if (cleanupExecutor == null) {
            synchronized (this) {
                if (cleanupExecutor == null) {
                    startMemoryCleanup();
                }
            }
        }
    }

    private void startMemoryCleanup() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jwt-blacklist-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
        log.info("In-memory JWT blacklist cleanup scheduler started");
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = memoryBlacklist.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            if (now > it.next().getValue()) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired blacklist entries, remaining: {}", removed, memoryBlacklist.size());
        }
    }
}
