package com.basebackend.cache.ratelimit;

import com.basebackend.cache.exception.CacheException;

/**
 * 限流超限异常
 * 当请求超出限流阈值时抛出
 */
public class RateLimitExceededException extends CacheException {

    private final String key;
    private final long retryAfterMillis;

    public RateLimitExceededException(String key, long retryAfterMillis) {
        super("Rate limit exceeded for key: " + key);
        this.key = key;
        this.retryAfterMillis = retryAfterMillis;
    }

    public RateLimitExceededException(String key) {
        this(key, -1);
    }

    public String getKey() {
        return key;
    }

    /**
     * 建议的重试等待时间（毫秒），-1 表示未知
     */
    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
