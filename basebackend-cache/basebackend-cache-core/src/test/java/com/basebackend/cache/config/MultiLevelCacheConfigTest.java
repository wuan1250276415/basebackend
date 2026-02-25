package com.basebackend.cache.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MultiLevelCacheConfig 多级缓存配置测试
 * 测试 Caffeine 缓存管理器的创建和配置
 */
@SpringBootTest(classes = {
    MultiLevelCacheConfig.class,
    CacheProperties.class
})
@TestPropertySource(properties = {
    "basebackend.cache.enabled=true",
    "basebackend.cache.multi-level.enabled=true",
    "basebackend.cache.multi-level.local-max-size=500",
    "basebackend.cache.multi-level.local-ttl=3m",
    "basebackend.cache.multi-level.eviction-policy=LRU"
})
class MultiLevelCacheConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired(required = false)
    private CaffeineCacheManager caffeineCacheManager;

    @Test
    void testMultiLevelCacheConfigLoaded() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("multiLevelCacheConfig"));
    }

    @Test
    void testCaffeineCacheManagerCreated() {
        assertNotNull(caffeineCacheManager);
    }

    @Test
    void testCaffeineCacheManagerConfiguration() {
        assertNotNull(caffeineCacheManager);
        // Verify the cache manager is properly configured
        assertNotNull(caffeineCacheManager.getCacheNames());
    }

    @Test
    void testMultiLevelPropertiesLoaded() {
        CacheProperties.MultiLevel multiLevel = cacheProperties.getMultiLevel();
        assertNotNull(multiLevel);
        assertTrue(multiLevel.getLocalMaxSize() > 0);
        assertNotNull(multiLevel.getEvictionPolicy());
    }
}
