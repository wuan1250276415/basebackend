package com.basebackend.common.ratelimit;

public interface RateLimiter {

    boolean tryAcquire(String key, int limit, int windowSeconds);
}
