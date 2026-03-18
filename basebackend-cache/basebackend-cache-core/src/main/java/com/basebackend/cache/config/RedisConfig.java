package com.basebackend.cache.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.basebackend.cache.serializer.PlainJsonRedisSerializer;

/**
 * Redis 配置
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * RedisTemplate 配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器处理 key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 复用现有 JsonCacheSerializer，保持 value/hashValue 的纯 JSON 线格式兼容
        PlainJsonRedisSerializer plainJsonRedisSerializer = new PlainJsonRedisSerializer();
        template.setValueSerializer(plainJsonRedisSerializer);
        template.setHashValueSerializer(plainJsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis 消息监听容器配置
     * 用于实现缓存失效通知的 Pub/Sub 机制
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
