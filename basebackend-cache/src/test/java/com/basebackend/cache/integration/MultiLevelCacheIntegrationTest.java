package com.basebackend.cache.integration;

import com.basebackend.cache.config.CacheAutoConfiguration;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.metrics.CacheStatistics;
import com.basebackend.cache.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级缓存集成测试
 * 测试本地缓存和 Redis 的协同工作
 */
@SpringBootTest(classes = {CacheAutoConfiguration.class})
@Testcontainers
class MultiLevelCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("basebackend.cache.enabled", () -> "true");
        registry.add("basebackend.cache.multi-level.enabled", () -> "true");
        registry.add("basebackend.cache.multi-level.local-max-size", () -> "100");
        registry.add("basebackend.cache.multi-level.local-ttl", () -> "5m");
        registry.add("basebackend.cache.multi-level.eviction-policy", () -> "LRU");
    }

    @Autowired
    private MultiLevelCacheManager cacheManager;

    @Autowired
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        // 清理缓存
        redisService.deleteByPattern("test:*");
        cacheManager.resetStatistics();
    }

    @Test
    void testLocalCacheHit() {
        // 测试本地缓存命中
        String key = "test:local:hit";
        String value = "test-value";

        // 第一次设置
        cacheManager.set(key, value);

        // 第二次获取应该命中本地缓存
        String result = cacheManager.get(key, String.class);
        assertEquals(value, result);

        // 验证本地缓存命中率
        assertTrue(cacheManager.getLocalHitRate() > 0);
    }

    @Test
    void testRedisHitAndLocalSync() {
        // 测试 Redis 命中并同步到本地缓存
        String key = "test:redis:sync";
        String value = "redis-value";

        // 直接在 Redis 中设置值（绕过本地缓存）
        redisService.set(key, value);

        // 第一次获取应该从 Redis 获取并同步到本地
        String result1 = cacheManager.get(key, String.class);
        assertEquals(value, result1);

        // 第二次获取应该命中本地缓存
        String result2 = cacheManager.get(key, String.class);
        assertEquals(value, result2);

        // 验证本地缓存已包含该值
        assertTrue(cacheManager.getLocalCacheSize() > 0);
    }

    @Test
    void testCacheEviction() {
        // 测试缓存删除
        String key = "test:evict:key";
        String value = "evict-value";

        // 设置缓存
        cacheManager.set(key, value);
        assertEquals(value, cacheManager.get(key, String.class));

        // 删除缓存
        cacheManager.evict(key);

        // 验证本地和 Redis 都已删除
        assertNull(cacheManager.get(key, String.class));
        assertNull(redisService.get(key));
    }

    @Test
    void testCacheStatistics() {
        // 测试缓存统计
        String key1 = "test:stats:1";
        String key2 = "test:stats:2";

        // 设置缓存
        cacheManager.set(key1, "value1");
        cacheManager.set(key2, "value2");

        // 多次获取以产生命中
        cacheManager.get(key1, String.class);
        cacheManager.get(key1, String.class);
        cacheManager.get(key2, String.class);

        // 获取不存在的键以产生未命中
        cacheManager.get("test:stats:nonexistent", String.class);

        // 获取统计信息
        CacheStatistics stats = cacheManager.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getHitCount() > 0);
        assertTrue(stats.getMissCount() > 0);
        assertTrue(stats.getHitRate() > 0);
    }

    @Test
    void testLRUEviction() throws InterruptedException {
        // 测试 LRU 淘汰策略
        // 注意：这个测试依赖于配置的 local-max-size=100

        // 填充超过最大容量的数据
        for (int i = 0; i < 150; i++) {
            cacheManager.set("test:lru:" + i, "value" + i);
        }

        // 等待一小段时间让淘汰生效
        Thread.sleep(100);

        // 验证本地缓存大小不超过最大容量
        long localSize = cacheManager.getLocalCacheSize();
        assertTrue(localSize <= 100, "Local cache size should not exceed max size: " + localSize);
    }

    @Test
    void testCacheTTL() throws InterruptedException {
        // 测试缓存过期
        String key = "test:ttl:key";
        String value = "ttl-value";

        // 设置2秒过期的缓存
        cacheManager.set(key, value, Duration.ofSeconds(2));

        // 立即获取应该存在
        assertEquals(value, cacheManager.get(key, String.class));

        // 等待3秒
        Thread.sleep(3000);

        // Redis 中应该已过期
        assertNull(redisService.get(key));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // 测试并发访问
        String key = "test:concurrent:key";
        String value = "concurrent-value";

        cacheManager.set(key, value);

        // 创建多个线程并发访问
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String result = cacheManager.get(key, String.class);
                results[index] = value.equals(result);
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有线程都获取到了正确的值
        for (boolean result : results) {
            assertTrue(result);
        }
    }

    @Test
    void testOverallHitRate() {
        // 测试整体命中率计算
        cacheManager.resetStatistics();

        // 设置一些缓存
        cacheManager.set("test:hit:1", "value1");
        cacheManager.set("test:hit:2", "value2");

        // 产生命中
        cacheManager.get("test:hit:1", String.class); // 本地命中
        cacheManager.get("test:hit:1", String.class); // 本地命中
        cacheManager.get("test:hit:2", String.class); // 本地命中

        // 产生未命中
        cacheManager.get("test:hit:nonexistent", String.class);

        // 验证命中率
        double hitRate = cacheManager.getOverallHitRate();
        assertTrue(hitRate > 0 && hitRate < 1.0);
    }
}
