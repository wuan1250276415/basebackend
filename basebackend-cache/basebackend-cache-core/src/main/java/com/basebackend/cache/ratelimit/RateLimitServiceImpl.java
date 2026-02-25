package com.basebackend.cache.ratelimit;

import com.basebackend.cache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式限流服务实现
 * 基于 Redisson RRateLimiter，使用 Redis 令牌桶算法
 */
@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private final RedissonClient redissonClient;
    private final CacheProperties cacheProperties;

    public RateLimitServiceImpl(RedissonClient redissonClient, CacheProperties cacheProperties) {
        this.redissonClient = redissonClient;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public boolean tryAcquire(String key, long rate, long interval, TimeUnit unit, RateType mode) {
        return tryAcquire(key, 1, rate, interval, unit, mode);
    }

    @Override
    public boolean tryAcquire(String key, long permits, long rate, long interval, TimeUnit unit, RateType mode) {
        String fullKey = resolveKey(key);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(fullKey);

        // trySetRate 仅在首次设置时生效，已存在则返回 false（幂等）
        RateIntervalUnit rateUnit = toRateIntervalUnit(unit);
        rateLimiter.trySetRate(mode, rate, interval, rateUnit);

        return rateLimiter.tryAcquire(permits);
    }

    @Override
    public long availablePermits(String key) {
        String fullKey = resolveKey(key);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(fullKey);
        return rateLimiter.availablePermits();
    }

    @Override
    public void deleteRateLimiter(String key) {
        String fullKey = resolveKey(key);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(fullKey);
        rateLimiter.delete();
        log.info("Deleted rate limiter: {}", fullKey);
    }

    private String resolveKey(String key) {
        String prefix = cacheProperties.getRateLimiter().getKeyPrefix();
        return prefix + key;
    }

    private RateIntervalUnit toRateIntervalUnit(TimeUnit unit) {
        return switch (unit) {
            case MILLISECONDS -> RateIntervalUnit.MILLISECONDS;
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
            default -> RateIntervalUnit.SECONDS;
        };
    }
}
