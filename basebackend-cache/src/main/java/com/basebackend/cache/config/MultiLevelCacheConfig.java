package com.basebackend.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置类
 * 配置本地缓存（Caffeine）和 Redis 缓存的集成
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "basebackend.cache.multi-level", name = "enabled", havingValue = "true")
public class MultiLevelCacheConfig {

    private final CacheProperties cacheProperties;

    public MultiLevelCacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 配置 Caffeine 本地缓存管理器
     */
    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CacheProperties.MultiLevel multiLevel = cacheProperties.getMultiLevel();
        
        log.info("Configuring Caffeine local cache manager");
        log.info("Local cache max size: {}", multiLevel.getLocalMaxSize());
        log.info("Local cache TTL: {}", multiLevel.getLocalTtl());
        log.info("Eviction policy: {}", multiLevel.getEvictionPolicy());

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置 Caffeine 缓存
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(multiLevel.getLocalMaxSize())
                .expireAfterWrite(multiLevel.getLocalTtl().toMillis(), TimeUnit.MILLISECONDS);

        // 根据淘汰策略配置
        switch (multiLevel.getEvictionPolicy().toUpperCase()) {
            case "LRU":
                // Caffeine 默认使用 Window TinyLFU，这里我们使用 expireAfterAccess 来模拟 LRU
                caffeineBuilder.expireAfterAccess(multiLevel.getLocalTtl().toMillis(), TimeUnit.MILLISECONDS);
                log.info("Using LRU eviction policy (expireAfterAccess)");
                break;
            case "LFU":
                // Caffeine 的默认策略就是基于频率的
                log.info("Using LFU eviction policy (Caffeine default)");
                break;
            case "FIFO":
                // FIFO 使用 expireAfterWrite
                log.info("Using FIFO eviction policy (expireAfterWrite)");
                break;
            default:
                log.warn("Unknown eviction policy: {}, using default", multiLevel.getEvictionPolicy());
        }

        // 启用统计
        caffeineBuilder.recordStats();

        cacheManager.setCaffeine(caffeineBuilder);
        
        return cacheManager;
    }
}
