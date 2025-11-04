package com.basebackend.scheduler.camunda.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流缓存配置
 */
@Configuration
@EnableCaching
public class WorkflowCacheConfig {

    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager workflowCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 默认缓存配置（5分钟过期）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 为不同类型的数据设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 流程定义缓存（10分钟）- 流程定义变化不频繁
        cacheConfigurations.put("processDefinitions", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 流程统计缓存（1分钟）- 统计数据实时性要求高
        cacheConfigurations.put("processStatistics", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // 表单模板缓存（30分钟）- 表单模板很少变化
        cacheConfigurations.put("formTemplates", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 历史流程实例缓存（5分钟）
        cacheConfigurations.put("historicProcessInstances", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
