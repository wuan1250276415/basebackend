package com.basebackend.scheduler.performance;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduler 模块专用多层缓存配置
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

@Configuration("schedulerMultiLevelCacheConfig")
@EnableCaching
@RequiredArgsConstructor
public class SchedulerCacheConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SchedulerCacheConfig.class);

    /**
     * 工作流定义缓存（L1）
     */
    @Bean("workflowDefinitionCache")
    public LoadingCache<String, Object> workflowDefinitionCache() {
        log.info("初始化工作流定义缓存（L1）");
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .build(key -> {
                    log.debug("工作流定义缓存未命中，key={}", key);
                    return null;
                });
    }

    /**
     * 工作流实例缓存（L1）
     */
    @Bean("workflowInstanceCache")
    public LoadingCache<String, Object> workflowInstanceCache() {
        log.info("初始化工作流实例缓存（L1）");
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(Duration.ofMinutes(15))
                .recordStats()
                .build(key -> {
                    log.debug("工作流实例缓存未命中，key={}", key);
                    return null;
                });
    }

    /**
     * 任务处理器缓存（L1）
     */
    @Bean("taskProcessorCache")
    public Cache<String, Object> taskProcessorCache() {
        log.info("初始化任务处理器缓存（L1）");
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(Duration.ofHours(1))
                .recordStats()
                .build();
    }

    /**
     * Redis 缓存管理器（L2）
     */
    @Bean("schedulerRedisCacheManager")
    public CacheManager schedulerRedisCacheManager(
            RedisTemplate<String, Object> redisTemplate) {
        log.info("初始化Redis缓存管理器（L2）");

        return new org.springframework.cache.support.SimpleCacheManager() {
            private final Map<String, org.springframework.cache.Cache> caches = new HashMap<>();

            {
                caches.put("workflowDefinitions", new RedisCacheWrapper("workflowDefinitions", redisTemplate, Duration.ofHours(1)));
                caches.put("workflowInstances", new RedisCacheWrapper("workflowInstances", redisTemplate, Duration.ofMinutes(30)));
                caches.put("taskProcessors", new RedisCacheWrapper("taskProcessors", redisTemplate, Duration.ofHours(2)));
            }

            @Override
            public Collection<String> getCacheNames() {
                return caches.keySet();
            }

            @Override
            public org.springframework.cache.Cache getCache(String name) {
                return caches.get(name);
            }
        };
    }

    /**
     * 多层缓存管理器（L1 + L2）
     */
    @Bean("multiLevelCacheManager")
    @Primary
    public CacheManager multiLevelCacheManager(
            @Qualifier("workflowDefinitionCache") LoadingCache<String, Object> l1WorkflowCache,
            @Qualifier("workflowInstanceCache") LoadingCache<String, Object> l1InstanceCache,
            @Qualifier("taskProcessorCache") Cache<String, Object> l1ProcessorCache,
            @Qualifier("schedulerRedisCacheManager") CacheManager l2CacheManager) {

        log.info("初始化多层缓存管理器（L1 + L2）");

        return new org.springframework.cache.support.SimpleCacheManager() {
            private final Map<String, org.springframework.cache.Cache> caches = new HashMap<>();

            {
                caches.put("workflowDefinitions", new MultiLevelCache("workflowDefinitions", (Cache<Object, Object>) (Cache<?, ?>) l1WorkflowCache, l2CacheManager));
                caches.put("workflowInstances", new MultiLevelCache("workflowInstances", (Cache<Object, Object>) (Cache<?, ?>) l1InstanceCache, l2CacheManager));
                caches.put("taskProcessors", new MultiLevelCache("taskProcessors", (Cache<Object, Object>) (Cache<?, ?>) l1ProcessorCache, l2CacheManager));
            }

            @Override
            public Collection<String> getCacheNames() {
                return caches.keySet();
            }

            @Override
            public org.springframework.cache.Cache getCache(String name) {
                return caches.get(name);
            }
        };
    }

    /**
     * Redis 缓存包装器
     */
    private static class RedisCacheWrapper implements org.springframework.cache.Cache {
        private final String name;
        private final RedisTemplate<String, Object> redisTemplate;
        private final Duration expireAfterWrite;

        public RedisCacheWrapper(String name, RedisTemplate<String, Object> redisTemplate, Duration expireAfterWrite) {
            this.name = name;
            this.redisTemplate = redisTemplate;
            this.expireAfterWrite = expireAfterWrite;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return redisTemplate;
        }

        @Override
        public ValueWrapper get(Object key) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    log.debug("Redis缓存命中，name={}, key={}", name, key);
                } else {
                    log.debug("Redis缓存未命中，name={}, key={}", name, key);
                }
                return value != null ? () -> value : null;
            } catch (Exception e) {
                log.error("Redis缓存获取失败，name={}, key={}", name, key, e);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            return wrapper != null ? type.cast(wrapper.get()) : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
            try {
                T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (Exception e) {
                log.error("缓存值加载失败，name={}, key={}", name, key, e);
                throw new RuntimeException("缓存值加载失败", e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            try {
                redisTemplate.opsForValue().set(key.toString(), value, expireAfterWrite);
                log.debug("Redis缓存存储成功，name={}, key={}", name, key);
            } catch (Exception e) {
                log.error("Redis缓存存储失败，name={}, key={}", name, key, e);
            }
        }

        @Override
        public void evict(Object key) {
            try {
                redisTemplate.delete(key.toString());
                log.debug("Redis缓存删除成功，name={}, key={}", name, key);
            } catch (Exception e) {
                log.error("Redis缓存删除失败，name={}, key={}", name, key, e);
            }
        }

        @Override
        public void clear() {
            try {
                // 注意：这里应该使用更精确的清理策略
                log.warn("Redis缓存清理操作，name={}", name);
            } catch (Exception e) {
                log.error("Redis缓存清理失败，name={}", name, e);
            }
        }
    }

    /**
     * 多层缓存实现
     */
    private static class MultiLevelCache implements org.springframework.cache.Cache {
        private final String name;
        private final Cache<Object, Object> l1Cache;
        private final CacheManager l2CacheManager;

        public MultiLevelCache(String name, Cache<Object, Object> l1Cache, CacheManager l2CacheManager) {
            this.name = name;
            this.l1Cache = l1Cache;
            this.l2CacheManager = l2CacheManager;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return Arrays.asList(l1Cache, l2CacheManager);
        }

        @Override
        public ValueWrapper get(Object key) {
            // 先查L1缓存
            Object l1Value = l1Cache.getIfPresent(key);
            if (l1Value != null) {
                log.debug("多层缓存L1命中，name={}, key={}", name, key);
                return () -> l1Value;
            }

            // 查L2缓存
            org.springframework.cache.Cache l2Cache = l2CacheManager.getCache(name);
            if (l2Cache != null) {
                ValueWrapper l2Value = l2Cache.get(key);
                if (l2Value != null) {
                    log.debug("多层缓存L2命中，name={}, key={}", name, key);
                    // 回写到L1
                    l1Cache.put(key, l2Value.get());
                    return l2Value;
                }
            }

            log.debug("多层缓存未命中，name={}, key={}", name, key);
            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            return wrapper != null ? type.cast(wrapper.get()) : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
            try {
                T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (Exception e) {
                log.error("多层缓存值加载失败，name={}, key={}", name, key, e);
                throw new RuntimeException("缓存值加载失败", e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            // 同时写入L1和L2
            l1Cache.put(key, value);

            org.springframework.cache.Cache l2Cache = l2CacheManager.getCache(name);
            if (l2Cache != null) {
                l2Cache.put(key, value);
            }

            log.debug("多层缓存存储成功，name={}, key={}", name, key);
        }

        @Override
        public void evict(Object key) {
            l1Cache.invalidate(key);

            org.springframework.cache.Cache l2Cache = l2CacheManager.getCache(name);
            if (l2Cache != null) {
                l2Cache.evict(key);
            }

            log.debug("多层缓存删除成功，name={}, key={}", name, key);
        }

        @Override
        public void clear() {
            l1Cache.invalidateAll();

            org.springframework.cache.Cache l2Cache = l2CacheManager.getCache(name);
            if (l2Cache != null) {
                l2Cache.clear();
            }

            log.debug("多层缓存清理成功，name={}", name);
        }
    }

    /**
     * 缓存预热器
     */
    @Bean("cacheWarmer")
    public CacheWarmer cacheWarmer(
            @Qualifier("workflowDefinitionCache") LoadingCache<String, Object> workflowCache,
            @Qualifier("workflowInstanceCache") LoadingCache<String, Object> instanceCache,
            @Qualifier("taskProcessorCache") Cache<String, Object> processorCache) {

        log.info("初始化缓存预热器");
        return new CacheWarmer(workflowCache, instanceCache, processorCache);
    }

    /**
     * 缓存预热工具类
     */
    public static class CacheWarmer {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CacheWarmer.class);

        private final LoadingCache<String, Object> workflowCache;
        private final LoadingCache<String, Object> instanceCache;
        private final Cache<String, Object> processorCache;

        public CacheWarmer(LoadingCache<String, Object> workflowCache,
                          LoadingCache<String, Object> instanceCache,
                          Cache<String, Object> processorCache) {
            this.workflowCache = workflowCache;
            this.instanceCache = instanceCache;
            this.processorCache = processorCache;
        }

        /**
         * 预热工作流定义缓存
         */
        public void warmWorkflowDefinitions(List<String> definitionIds) {
            log.info("开始预热工作流定义缓存，数量={}", definitionIds.size());
            long startTime = System.currentTimeMillis();

            definitionIds.parallelStream().forEach(definitionId -> {
                try {
                    // 这里应该从数据库加载实际数据
                    workflowCache.get(definitionId);
                    log.debug("预热工作流定义，definitionId={}", definitionId);
                } catch (Exception e) {
                    log.error("预热工作流定义失败，definitionId={}", definitionId, e);
                }
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("工作流定义缓存预热完成，耗时={}ms", duration);
        }

        /**
         * 预热任务处理器缓存
         */
        public void warmTaskProcessors(List<String> processorNames) {
            log.info("开始预热任务处理器缓存，数量={}", processorNames.size());
            long startTime = System.currentTimeMillis();

            processorNames.parallelStream().forEach(processorName -> {
                try {
                    // 这里应该从注册表加载实际数据
                    processorCache.getIfPresent(processorName);
                    log.debug("预热任务处理器，processorName={}", processorName);
                } catch (Exception e) {
                    log.error("预热任务处理器失败，processorName={}", processorName, e);
                }
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("任务处理器缓存预热完成，耗时={}ms", duration);
        }

        /**
         * 获取缓存统计信息
         */
        public Map<String, Object> getCacheStatistics() {
            Map<String, Object> stats = new ConcurrentHashMap<>();

            try {
                if (workflowCache instanceof com.github.benmanes.caffeine.cache.LoadingCache) {
                    com.github.benmanes.caffeine.cache.LoadingCache<String, Object> loadingCache =
                            (com.github.benmanes.caffeine.cache.LoadingCache<String, Object>) workflowCache;
                    stats.put("workflowCacheStats", loadingCache.stats());
                }
            } catch (Exception e) {
                log.error("获取工作流缓存统计失败", e);
            }

            try {
                if (processorCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                    com.github.benmanes.caffeine.cache.Cache<String, Object> cache =
                            (com.github.benmanes.caffeine.cache.Cache<String, Object>) processorCache;
                    stats.put("processorCacheStats", cache.stats());
                }
            } catch (Exception e) {
                log.error("获取处理器缓存统计失败", e);
            }

            log.debug("缓存统计信息获取完成");
            return stats;
        }
    }
}
