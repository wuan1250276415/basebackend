package com.basebackend.common.ratelimit.config;

import com.basebackend.common.ratelimit.RateLimiter;
import com.basebackend.common.ratelimit.aspect.RateLimitAspect;
import com.basebackend.common.ratelimit.impl.SlidingWindowRateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "basebackend.common.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(RateLimitProperties properties) {
        return new SlidingWindowRateLimiter(properties.getMaxKeys(), properties.getCleanupIntervalMinutes());
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimiter rateLimiter) {
        return new RateLimitAspect(rateLimiter);
    }
}
