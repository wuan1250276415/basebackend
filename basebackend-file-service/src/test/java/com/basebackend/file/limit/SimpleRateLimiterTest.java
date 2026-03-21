package com.basebackend.file.limit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimpleRateLimiter 测试")
class SimpleRateLimiterTest {

    @Test
    @DisplayName("滑动窗口策略在默认内存限流器中可用")
    void shouldSupportSlidingWindowPolicy() {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter();
        RateLimitPolicy policy = RateLimitPolicy.slidingWindowLimit(1, 2, TimeUnit.SECONDS);

        assertThat(rateLimiter.isAllowed("upload:user-1", policy)).isTrue();
        assertThat(rateLimiter.isAllowed("upload:user-1", policy)).isTrue();
        assertThat(rateLimiter.isAllowed("upload:user-1", policy)).isFalse();
    }
}
