package com.basebackend.security.service;

import com.basebackend.security.config.SecurityBaselineProperties;
import com.basebackend.security.service.impl.RedisAuthenticationRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisAuthenticationRateLimiter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAuthenticationRateLimiter 认证速率限制测试")
class RedisAuthenticationRateLimiterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisAuthenticationRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        SecurityBaselineProperties properties = new SecurityBaselineProperties();
        SecurityBaselineProperties.RateLimitConfig config = properties.getRateLimit();
        config.setMaxAttempts(3);
        config.setBlockDuration(Duration.ofMinutes(10));
        config.setWindowDuration(Duration.ofMinutes(5));

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiter = new RedisAuthenticationRateLimiter(redisTemplate, properties);
    }

    @Test
    @DisplayName("首次尝试应允许通过")
    void shouldAllowFirstAttempt() {
        when(redisTemplate.hasKey("auth:ratelimit:block:192.168.1.1")).thenReturn(false);
        when(valueOperations.increment("auth:ratelimit:attempt:192.168.1.1")).thenReturn(1L);

        boolean result = rateLimiter.tryAcquire("192.168.1.1");

        assertThat(result).isTrue();
        verify(redisTemplate).expire(eq("auth:ratelimit:attempt:192.168.1.1"), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("未超过阈值应允许通过")
    void shouldAllowBelowThreshold() {
        when(redisTemplate.hasKey("auth:ratelimit:block:192.168.1.1")).thenReturn(false);
        when(valueOperations.increment("auth:ratelimit:attempt:192.168.1.1")).thenReturn(3L);

        boolean result = rateLimiter.tryAcquire("192.168.1.1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("超过阈值应拒绝并封禁")
    void shouldBlockWhenThresholdExceeded() {
        when(redisTemplate.hasKey("auth:ratelimit:block:192.168.1.1")).thenReturn(false);
        when(valueOperations.increment("auth:ratelimit:attempt:192.168.1.1")).thenReturn(4L);

        boolean result = rateLimiter.tryAcquire("192.168.1.1");

        assertThat(result).isFalse();
        verify(valueOperations).set(eq("auth:ratelimit:block:192.168.1.1"), eq("1"), eq(600L), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete("auth:ratelimit:attempt:192.168.1.1");
    }

    @Test
    @DisplayName("已封禁的 key 应直接拒绝")
    void shouldRejectBlockedKey() {
        when(redisTemplate.hasKey("auth:ratelimit:block:192.168.1.1")).thenReturn(true);

        boolean result = rateLimiter.tryAcquire("192.168.1.1");

        assertThat(result).isFalse();
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @DisplayName("isBlocked 应正确检查封禁状态")
    void shouldCheckBlockedStatus() {
        when(redisTemplate.hasKey("auth:ratelimit:block:test-key")).thenReturn(true);

        assertThat(rateLimiter.isBlocked("test-key")).isTrue();
    }

    @Test
    @DisplayName("resetAttempts 应删除计数器")
    void shouldResetAttempts() {
        rateLimiter.resetAttempts("192.168.1.1");

        verify(redisTemplate).delete("auth:ratelimit:attempt:192.168.1.1");
    }

    @Test
    @DisplayName("getRemainingBlockSeconds 应返回剩余封禁时间")
    void shouldReturnRemainingBlockSeconds() {
        when(redisTemplate.getExpire("auth:ratelimit:block:192.168.1.1", TimeUnit.SECONDS)).thenReturn(120L);

        assertThat(rateLimiter.getRemainingBlockSeconds("192.168.1.1")).isEqualTo(120L);
    }

    @Test
    @DisplayName("Redis 不可用时应放行（fail-open for rate limiting）")
    void shouldAllowWhenRedisUnavailable() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));

        boolean result = rateLimiter.tryAcquire("192.168.1.1");

        assertThat(result).isTrue();
    }
}
