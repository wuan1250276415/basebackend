package com.basebackend.common.ratelimit.config;

import com.basebackend.common.ratelimit.RateLimitAlgorithm;
import com.basebackend.common.ratelimit.RateLimiter;
import com.basebackend.common.ratelimit.RateLimiterRegistry;
import com.basebackend.common.ratelimit.aspect.RateLimitAspect;
import com.basebackend.common.ratelimit.impl.FixedWindowRateLimiter;
import com.basebackend.common.ratelimit.impl.RedisSlidingWindowRateLimiter;
import com.basebackend.common.ratelimit.impl.RedisTokenBucketRateLimiter;
import com.basebackend.common.ratelimit.impl.SlidingWindowRateLimiter;
import com.basebackend.common.ratelimit.impl.TokenBucketRateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.EnumMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "basebackend.common.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    /**
     * 内存模式限流器配置（默认）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "basebackend.common.ratelimit", name = "type", havingValue = "memory", matchIfMissing = true)
    static class MemoryRateLimitConfiguration {

        @Bean
        @ConditionalOnMissingBean(RateLimiterRegistry.class)
        public RateLimiterRegistry rateLimiterRegistry(RateLimitProperties properties) {
            Map<RateLimitAlgorithm, RateLimiter> limiters = new EnumMap<>(RateLimitAlgorithm.class);
            limiters.put(RateLimitAlgorithm.SLIDING_WINDOW,
                    new SlidingWindowRateLimiter(properties.getMaxKeys(), properties.getCleanupIntervalMinutes()));
            limiters.put(RateLimitAlgorithm.TOKEN_BUCKET, new TokenBucketRateLimiter());
            limiters.put(RateLimitAlgorithm.FIXED_WINDOW, new FixedWindowRateLimiter());
            return new RateLimiterRegistry(properties.getAlgorithm(), limiters);
        }
    }

    /**
     * Redis 模式限流器配置
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "basebackend.common.ratelimit", name = "type", havingValue = "redis")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnBean(StringRedisTemplate.class)
    static class RedisRateLimitConfiguration {

        @Bean
        @ConditionalOnMissingBean(RateLimiterRegistry.class)
        public RateLimiterRegistry rateLimiterRegistry(RateLimitProperties properties,
                                                       StringRedisTemplate redisTemplate) {
            Map<RateLimitAlgorithm, RateLimiter> limiters = new EnumMap<>(RateLimitAlgorithm.class);
            limiters.put(RateLimitAlgorithm.SLIDING_WINDOW,
                    new RedisSlidingWindowRateLimiter(redisTemplate, properties.isAllowOnRedisFailure()));
            limiters.put(RateLimitAlgorithm.TOKEN_BUCKET,
                    new RedisTokenBucketRateLimiter(redisTemplate, properties.isAllowOnRedisFailure()));
            // Redis 没有固定窗口实现，回退到内存版
            limiters.put(RateLimitAlgorithm.FIXED_WINDOW, new FixedWindowRateLimiter());
            return new RateLimiterRegistry(properties.getAlgorithm(), limiters);
        }
    }

    /**
     * 向后兼容：如果用户自定义了 RateLimiter Bean 但没有定义 Registry
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiterRegistry.class)
    @ConditionalOnBean(RateLimiter.class)
    public RateLimiterRegistry fallbackRateLimiterRegistry(RateLimiter rateLimiter) {
        return RateLimiterRegistry.ofSingle(rateLimiter);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimiterRegistry rateLimiterRegistry) {
        return new RateLimitAspect(rateLimiterRegistry);
    }
}
