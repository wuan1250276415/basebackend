package com.basebackend.common.ratelimit.impl;

import com.basebackend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌桶限流器（内存版）
 * <p>
 * 令牌以固定速率填充，每个请求消耗一个令牌。
 * 参数映射：limit = 桶容量，windowSeconds = 完全填满桶的时间。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        if (!isValidParams(key, limit, windowSeconds)) {
            return false;
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(limit, windowSeconds));
        return bucket.tryConsume(limit, windowSeconds);
    }

    private boolean isValidParams(String key, int limit, int windowSeconds) {
        if (key == null) {
            log.warn("TokenBucketRateLimiter key 不能为空");
            return false;
        }
        if (limit <= 0 || windowSeconds <= 0) {
            log.warn("TokenBucketRateLimiter 参数非法: key={}, limit={}, windowSeconds={}", key, limit, windowSeconds);
            return false;
        }
        return true;
    }

    private static class Bucket {
        private double tokens;
        private long lastRefillTimestamp;
        private final Object lock = new Object();

        Bucket(int capacity, int windowSeconds) {
            this.tokens = capacity;
            this.lastRefillTimestamp = System.nanoTime();
        }

        boolean tryConsume(int capacity, int windowSeconds) {
            synchronized (lock) {
                refill(capacity, windowSeconds);
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return true;
                }
                return false;
            }
        }

        private void refill(int capacity, int windowSeconds) {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1_000_000_000.0;
            double rate = (double) capacity / windowSeconds;
            double newTokens = elapsedSeconds * rate;
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTimestamp = now;
            }
        }
    }
}
