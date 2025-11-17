package com.basebackend.examples.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 多级缓存配置
 * 实现 L1 (Caffeine) + L2 (Redis) 多级缓存架构
 */
@Slf4j
@Configuration
public class MultiLevelCacheConfig {

    // ========================================
    // 缓存配置 Bean
    // ========================================

    /**
     * 用户缓存 (L1)
     */
    @Bean("userCacheL1")
    public Cache<String, Object> userCacheL1() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .expireAfterAccess(Duration.ofMinutes(5))
            .recordStats()
            .removalListener((key, value, cause) ->
                log.debug("用户缓存移除: key={}, cause={}", key, cause))
            .build();
    }

    /**
     * 菜单缓存 (L1)
     */
    @Bean("menuCacheL1")
    public Cache<String, Object> menuCacheL1() {
        return Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .recordStats()
            .build();
    }

    /**
     * 权限缓存 (L1)
     */
    @Bean("permissionCacheL1")
    public Cache<String, Object> permissionCacheL1() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .expireAfterAccess(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }

    /**
     * 字典缓存 (L1)
     */
    @Bean("dictCacheL1")
    public Cache<String, Object> dictCacheL1() {
        return Caffeine.newBuilder()
            .initialCapacity(20)
            .maximumSize(200)
            .expireAfterWrite(Duration.ofMinutes(60))
            .expireAfterAccess(Duration.ofMinutes(30))
            .recordStats()
            .build();
    }

    /**
     * 用户配置缓存 (L1)
     */
    @Bean("userProfileCacheL1")
    public Cache<String, Object> userProfileCacheL1() {
        return Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(20))
            .expireAfterAccess(Duration.ofMinutes(10))
            .recordStats()
            .build();
    }

    /**
     * 热点数据缓存 (L1)
     */
    @Bean("hotDataCacheL1")
    public Cache<String, Object> hotDataCacheL1() {
        return Caffeine.newBuilder()
            .initialCapacity(200)
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .expireAfterAccess(Duration.ofMinutes(2))
            .recordStats()
            .build();
    }
}
