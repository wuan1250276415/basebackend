package com.basebackend.common.ratelimit.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPropertiesTest {

    @Test
    @DisplayName("默认 Redis 异常回退策略为拒绝")
    void shouldDenyOnRedisFailureByDefault() {
        RateLimitProperties properties = new RateLimitProperties();
        assertThat(properties.isAllowOnRedisFailure()).isFalse();
    }

    @Test
    @DisplayName("可配置 Redis 异常回退为放行")
    void shouldAllowSwitchingRedisFailureFallback() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAllowOnRedisFailure(true);

        assertThat(properties.isAllowOnRedisFailure()).isTrue();
    }
}
