package com.basebackend.cache.invalidation;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.service.CacheService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 跨服务缓存失效自动配置
 * 注册 Redis Pub/Sub 监听器和发布器
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "basebackend.cache.invalidation", name = "enabled", havingValue = "true")
public class CacheInvalidationAutoConfiguration {

    @Bean
    public CacheInvalidationPublisher cacheInvalidationPublisher(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        log.info("Registering CacheInvalidationPublisher: channel={}, serviceName={}",
                cacheProperties.getInvalidation().getChannel(),
                cacheProperties.getInvalidation().getServiceName());
        return new CacheInvalidationPublisher(redisTemplate, cacheProperties, meterRegistry);
    }

    @Bean
    public CacheInvalidationListener cacheInvalidationListener(
            CacheService cacheService,
            CacheProperties cacheProperties,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        log.info("Registering CacheInvalidationListener");
        return new CacheInvalidationListener(cacheService, cacheProperties, multiLevelCacheManager, meterRegistry);
    }

    @Bean
    public RedisMessageListenerContainer cacheInvalidationListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationListener listener,
            CacheProperties cacheProperties) {
        String channel = cacheProperties.getInvalidation().getChannel();

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(channel));

        log.info("Redis Pub/Sub listener registered on channel: {}", channel);
        return container;
    }
}
