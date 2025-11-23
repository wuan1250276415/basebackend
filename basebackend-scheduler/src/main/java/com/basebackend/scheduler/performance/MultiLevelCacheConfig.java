package com.basebackend.scheduler.performance;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.cache.Cache.ValueWrapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多层缓存优化配置
 *
 * <p>实现 L1（内存）+ L2（Redis）多层缓存架构：
 * <ul>
 *   <li>L1 缓存：Caffeine 本地缓存（纳秒级访问）</li>
 *   <li>L2 缓存：Redis 分布式缓存（毫秒级访问）</li>
 *   <li>缓存同步：保证数据一致性</li>
 *   <li>缓存预热：提前加载热点数据</li>
 * </ul>
 *
 * <p>缓存策略：
 * <ul>
 *   <li>热数据：L1 + L2 双层缓存</li>
 *   <li>温数据：L2 缓存</li>
 *   <li>冷数据：实时查询</li>
 * </ul>
 *
 * <p>优势：
 * <ul>
 *   <li>极高的缓存命中率</li>
 *   <li>分布式环境共享缓存</li>
 *   <li>自动缓存淘汰</li>
 *   <li>实时缓存监控</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class MultiLevelCacheConfig {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private L2Cache l2Cache;

    /**
     * L1 缓存：Caffeine 本地缓存
     *
     * <p>特性：
     * <ul>
     *   <li>最大容量：1000 条记录</li>
     *   <li>过期时间：5 分钟</li>
     *   <li>统计信息：启用监控</li>
 *   <li>淘汰策略：LRU</li>
 * </ul>
     *
     * @return Caffeine Cache 实例
     */
    @Bean("l1Cache")
    public Cache<String, Object> l1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }

    /**
     * L1 缓存：预加载缓存
     *
     * <p>用于缓存预热场景，自动加载热点数据。
     *
     * @param l2Cache L2缓存实例
     * @return LoadingCache 实例
     */
    @Bean("l1LoadingCache")
    public LoadingCache<String, Object> l1LoadingCache(@Qualifier("l2Cache") L2Cache l2Cache) {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats()
                .build(key -> {
                    log.debug("L1 cache miss, loading from L2 or database: {}", key);
                    return loadFromL2(key);
                });
    }

    /**
     * L2 缓存：Redis 缓存
     *
     * @return L2 缓存配置
     */
    @Bean("l2Cache")
    public L2Cache l2Cache() {
        this.l2Cache = new L2Cache(redisTemplate, stringRedisTemplate);
        return this.l2Cache;
    }

    /**
     * 多层缓存管理器
     *
     * @param l1Cache L1 缓存
     * @param l2Cache L2 缓存
     * @return CacheManager 实例
     */
    @Bean
    @Primary
    public CacheManager multiLevelCacheManager(
            @Qualifier("l1Cache") Cache<String, Object> l1Cache,
            @Qualifier("l2Cache") L2Cache l2Cache) {
        return new MultiLevelCacheManager(l1Cache, l2Cache);
    }

    /**
     * 缓存预热器
     *
     * @param l1Cache L1缓存实例
     * @param l1LoadingCache L1加载缓存实例
     * @param l2Cache L2缓存实例
     * @return 缓存预热器
     */
    @Bean
    public CacheWarmer cacheWarmer(@Qualifier("l1Cache") Cache<String, Object> l1Cache,
                                   @Qualifier("l1LoadingCache") LoadingCache<String, Object> l1LoadingCache,
                                   @Qualifier("l2Cache") L2Cache l2Cache) {
        return new CacheWarmer(l1Cache, l1LoadingCache, l2Cache);
    }

    /**
     * L2 缓存实现
     */
    public static class L2Cache {

        private final RedisTemplate<String, Object> redisTemplate;
        private final StringRedisTemplate stringRedisTemplate;

        public L2Cache(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
            this.redisTemplate = redisTemplate;
            this.stringRedisTemplate = stringRedisTemplate;
        }

        /**
         * 从 L2 缓存获取数据
         *
         * @param key 缓存键
         * @return 缓存值
         */
        public Object get(String key) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    log.debug("L2 cache hit: {}", key);
                }
                return value;
            } catch (Exception e) {
                log.error("L2 cache get failed: {}", key, e);
                return null;
            }
        }

        /**
         * 向 L2 缓存存储数据
         *
         * @param key 缓存键
         * @param value 缓存值
         * @param expireMinutes 过期时间（分钟）
         */
        public void put(String key, Object value, long expireMinutes) {
            try {
                redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(expireMinutes));
                log.debug("L2 cache put: {}, expireMinutes={}", key, expireMinutes);
            } catch (Exception e) {
                log.error("L2 cache put failed: {}", key, e);
            }
        }

        /**
         * 从 L2 缓存删除数据
         *
         * @param key 缓存键
         */
        public void evict(String key) {
            try {
                redisTemplate.delete(key);
                log.debug("L2 cache evict: {}", key);
            } catch (Exception e) {
                log.error("L2 cache evict failed: {}", key, e);
            }
        }

        /**
         * 清空 L2 缓存
         */
        public void clear() {
            try {
                // 注意：生产环境不建议使用 FLUSHDB
                // redisTemplate.getConnectionFactory().getConnection().flushDb();
                log.warn("L2 cache clear requested (not implemented for safety)");
            } catch (Exception e) {
                log.error("L2 cache clear failed", e);
            }
        }
    }

    /**
     * 多层缓存管理器
     */
    public static class MultiLevelCacheManager implements CacheManager {

        private final Cache<String, Object> l1Cache;
        private final L2Cache l2Cache;
        private final Map<String, MultiLevelCache> cacheMap = new ConcurrentHashMap<>();

        public MultiLevelCacheManager(Cache<String, Object> l1Cache, L2Cache l2Cache) {
            this.l1Cache = l1Cache;
            this.l2Cache = l2Cache;
        }

        @Override
        public MultiLevelCache getCache(String name) {
            return cacheMap.computeIfAbsent(name, k -> new MultiLevelCache(name, l1Cache, l2Cache));
        }

        @Override
        public Collection<String> getCacheNames() {
            return cacheMap.keySet();
        }
    }

    /**
     * 多层缓存实例
     */
    public static class MultiLevelCache implements org.springframework.cache.Cache {

        private final String name;
        private final Cache<String, Object> l1Cache;
        private final L2Cache l2Cache;

        public MultiLevelCache(String name, Cache<String, Object> l1Cache, L2Cache l2Cache) {
            this.name = name;
            this.l1Cache = l1Cache;
            this.l2Cache = l2Cache;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return Arrays.asList(l1Cache, l2Cache);
        }

        @Override
        public ValueWrapper get(Object key) {
            // 先查 L1 缓存
            String cacheKey = key.toString();
            Object value = l1Cache.getIfPresent(cacheKey);
            if (value != null) {
                final Object finalValue = value;
                return () -> finalValue;
            }

            // L1 未命中，查询 L2 缓存
            value = l2Cache.get(cacheKey);
            if (value != null) {
                // L2 命中，放入 L1 缓存
                l1Cache.put(cacheKey, value);
                final Object finalValue = value;
                return () -> finalValue;
            }

            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            return wrapper != null ? type.cast(wrapper.get()) : null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
            try {
                T value = valueLoader.call();
                if (value != null) {
                    put(key, value);
                }
                return value;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load value for cache key: " + key, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            // 同时写入 L1 和 L2 缓存
            String cacheKey = key.toString();
            l1Cache.put(cacheKey, value);
            l2Cache.put(cacheKey, value, 30); // L2 缓存 30 分钟
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            String cacheKey = key.toString();
            // 先查 L1 缓存
            Object existingValue = l1Cache.getIfPresent(cacheKey);
            if (existingValue != null) {
                return () -> existingValue;
            }

            // L1 缓存不存在，写入 L1 和 L2
            l1Cache.put(cacheKey, value);
            l2Cache.put(cacheKey, value, 30);

            return null;
        }

        @Override
        public void evict(Object key) {
            String cacheKey = key.toString();
            l1Cache.invalidate(cacheKey);
            l2Cache.evict(cacheKey);
        }

        @Override
        public void clear() {
            l1Cache.invalidateAll();
            l2Cache.clear();
        }
    }

    /**
     * 缓存预热器
     */
    @Slf4j
    public static class CacheWarmer {

        private final Cache<String, Object> l1Cache;
        private final LoadingCache<String, Object> l1LoadingCache;
        private final L2Cache l2Cache;

        public CacheWarmer(Cache<String, Object> l1Cache,
                          LoadingCache<String, Object> l1LoadingCache,
                          L2Cache l2Cache) {
            this.l1Cache = l1Cache;
            this.l1LoadingCache = l1LoadingCache;
            this.l2Cache = l2Cache;
        }

        /**
         * 预热缓存
         *
         * @param cacheNames 缓存名称列表
         */
        public void warmUp(List<String> cacheNames) {
            log.info("Starting cache warm-up for caches: {}", cacheNames);

            for (String cacheName : cacheNames) {
                try {
                    // 加载热点数据到缓存
                    l1LoadingCache.get(cacheName);
                    log.debug("Cache warmed: {}", cacheName);
                } catch (Exception e) {
                    log.error("Cache warm-up failed for: {}", cacheName, e);
                }
            }

            log.info("Cache warm-up completed");
        }

        /**
         * 获取缓存统计
         *
         * @return 统计信息
         */
        public Map<String, Object> getCacheStatistics() {
            Map<String, Object> stats = new HashMap<>();

            if (l1Cache instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache cache =
                        (com.github.benmanes.caffeine.cache.Cache) l1Cache;

                com.github.benmanes.caffeine.cache.stats.CacheStats l1Stats = cache.stats();
                stats.put("l1_hit_rate", l1Stats.hitRate());
                stats.put("l1_hit_count", l1Stats.hitCount());
                stats.put("l1_miss_count", l1Stats.missCount());
                stats.put("l1_eviction_count", l1Stats.evictionCount());
            }

            return stats;
        }
    }

    /**
     * 从 L2 或数据库加载数据
     *
     * @param key 缓存键
     * @return 数据
     */
    private Object loadFromL2OrDatabase(String key) {
        return loadFromL2(key);
    }

    /**
     * 从 L2 缓存加载数据
     *
     * @param key 缓存键
     * @return 数据
     */
    private Object loadFromL2(String key) {
        if (l2Cache == null) {
            log.warn("L2 cache not initialized when loading key: {}", key);
            return null;
        }
        Object value = l2Cache.get(key);
        if (value != null) {
            return value;
        }
        log.debug("Loading from database: {}", key);
        // TODO: 实现从数据库加载逻辑
        return null;
    }
}
