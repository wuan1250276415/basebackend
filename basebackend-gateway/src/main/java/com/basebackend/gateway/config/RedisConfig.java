package com.basebackend.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {

    /**
     * 配置ReactiveRedisTemplate
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext(RedisSerializer.string())
                .value(RedisSerializer.json())
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
