package com.basebackend.common.ratelimit.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class RedisRateLimiterFallbackTest {

    @Test
    @DisplayName("滑动窗口在 Redis 异常时默认拒绝")
    void slidingWindowShouldDenyByDefaultWhenRedisFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any());

        RedisSlidingWindowRateLimiter limiter = new RedisSlidingWindowRateLimiter(redisTemplate);

        assertThat(limiter.tryAcquire("sw-fallback-default", 10, 60)).isFalse();
    }

    @Test
    @DisplayName("滑动窗口在 Redis 异常时可配置放行")
    void slidingWindowShouldAllowWhenConfiguredAndRedisFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any());

        RedisSlidingWindowRateLimiter limiter = new RedisSlidingWindowRateLimiter(redisTemplate, true);

        assertThat(limiter.tryAcquire("sw-fallback-allow", 10, 60)).isTrue();
    }

    @Test
    @DisplayName("令牌桶在 Redis 异常时默认拒绝")
    void tokenBucketShouldDenyByDefaultWhenRedisFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any());

        RedisTokenBucketRateLimiter limiter = new RedisTokenBucketRateLimiter(redisTemplate);

        assertThat(limiter.tryAcquire("tb-fallback-default", 10, 60)).isFalse();
    }

    @Test
    @DisplayName("令牌桶在 Redis 异常时可配置放行")
    void tokenBucketShouldAllowWhenConfiguredAndRedisFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any());

        RedisTokenBucketRateLimiter limiter = new RedisTokenBucketRateLimiter(redisTemplate, true);

        assertThat(limiter.tryAcquire("tb-fallback-allow", 10, 60)).isTrue();
    }
}
