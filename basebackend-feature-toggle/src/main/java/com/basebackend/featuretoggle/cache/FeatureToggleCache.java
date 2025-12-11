package com.basebackend.featuretoggle.cache;

import com.basebackend.featuretoggle.model.FeatureContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 特性开关缓存管理器
 * <p>
 * 使用Caffeine提供高性能的本地缓存，减少对外部特性开关服务的调用。
 * 支持多级缓存、自动刷新、统计等功能。
 * </p>
 *
 * <h3>缓存策略：</h3>
 * <ul>
 *   <li>特性状态缓存 - 缓存特性开关的启用状态</li>
 *   <li>变体信息缓存 - 缓存特性变体配置信息</li>
 *   <li>分组分配缓存 - 缓存AB测试分组分配结果</li>
 *   <li>自动过期 - 支持TTL自动过期</li>
 *   <li>统计监控 - 提供缓存命中率和性能统计</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class FeatureToggleCache {

    // 手动添加 Logger 以解决 Lombok 注解处理问题
    private static final Logger log = LoggerFactory.getLogger(FeatureToggleCache.class);

    // 缓存名称常量
    public static final String FEATURE_STATE_CACHE = "featureToggle:state";
    public static final String VARIANT_CACHE = "featureToggle:variant";
    public static final String GROUP_ASSIGNMENT_CACHE = "featureToggle:group";
    public static final String ROLLOUT_CACHE = "featureToggle:rollout";

    // 默认过期时间（秒）
    private static final long DEFAULT_TTL_SECONDS = 300; // 5分钟
    private static final long MAXIMUM_SIZE = 10000; // 最大缓存条目数

    private final CacheManager cacheManager;

    public FeatureToggleCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 获取特性开关状态（带缓存）
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param loader 数据加载器
     * @return 特性状态
     */
    public boolean getFeatureState(String featureName, FeatureContext context,
                                   Callable<Boolean> loader) {
        String cacheKey = generateStateCacheKey(featureName, context);
        Cache cache = getCache(FEATURE_STATE_CACHE);

        try {
            Boolean cachedValue = cache.get(cacheKey, Boolean.class);
            if (cachedValue != null) {
                log.trace("Cache HIT for feature state: {}", cacheKey);
                return cachedValue;
            }

            log.trace("Cache MISS for feature state: {}", cacheKey);
            Boolean value = loader.call();
            cache.put(cacheKey, value);
            return value;
        } catch (Exception e) {
            log.error("Error loading feature state from cache: {}", cacheKey, e);
            // 缓存失败时，直接调用加载器
            try {
                return loader.call();
            } catch (Exception ex) {
                log.error("Error loading feature state: {}", cacheKey, ex);
                return false; // 默认返回关闭状态
            }
        }
    }

    /**
     * 获取特性变体信息（带缓存）
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param loader 数据加载器
     * @return 变体信息
     */
    public String getVariant(String featureName, FeatureContext context,
                            Callable<String> loader) {
        String cacheKey = generateVariantCacheKey(featureName, context);
        Cache cache = getCache(VARIANT_CACHE);

        try {
            String cachedValue = cache.get(cacheKey, String.class);
            if (cachedValue != null) {
                log.trace("Cache HIT for variant: {}", cacheKey);
                return cachedValue;
            }

            log.trace("Cache MISS for variant: {}", cacheKey);
            String value = loader.call();
            cache.put(cacheKey, value);
            return value;
        } catch (Exception e) {
            log.error("Error loading variant from cache: {}", cacheKey, e);
            try {
                return loader.call();
            } catch (Exception ex) {
                log.error("Error loading variant: {}", cacheKey, ex);
                return "default";
            }
        }
    }

    /**
     * 获取AB测试分组分配（带缓存）
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param loader 数据加载器
     * @return 分组名称
     */
    public String getGroupAssignment(String featureName, FeatureContext context,
                                     Callable<String> loader) {
        String cacheKey = generateGroupCacheKey(featureName, context);
        Cache cache = getCache(GROUP_ASSIGNMENT_CACHE);

        try {
            String cachedValue = cache.get(cacheKey, String.class);
            if (cachedValue != null) {
                log.trace("Cache HIT for group assignment: {}", cacheKey);
                return cachedValue;
            }

            log.trace("Cache MISS for group assignment: {}", cacheKey);
            String value = loader.call();
            cache.put(cacheKey, value);
            return value;
        } catch (Exception e) {
            log.error("Error loading group assignment from cache: {}", cacheKey, e);
            try {
                return loader.call();
            } catch (Exception ex) {
                log.error("Error loading group assignment: {}", cacheKey, ex);
                return "control";
            }
        }
    }

    /**
     * 获取渐进式发布状态（带缓存）
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param loader 数据加载器
     * @return 发布状态
     */
    public boolean getRolloutState(String featureName, FeatureContext context,
                                   Callable<Boolean> loader) {
        String cacheKey = generateRolloutCacheKey(featureName, context);
        Cache cache = getCache(ROLLOUT_CACHE);

        try {
            Boolean cachedValue = cache.get(cacheKey, Boolean.class);
            if (cachedValue != null) {
                log.trace("Cache HIT for rollout state: {}", cacheKey);
                return cachedValue;
            }

            log.trace("Cache MISS for rollout state: {}", cacheKey);
            Boolean value = loader.call();
            cache.put(cacheKey, value);
            return value;
        } catch (Exception e) {
            log.error("Error loading rollout state from cache: {}", cacheKey, e);
            try {
                return loader.call();
            } catch (Exception ex) {
                log.error("Error loading rollout state: {}", cacheKey, ex);
                return false;
            }
        }
    }

    /**
     * 主动刷新缓存
     *
     * @param cacheName 缓存名称
     * @param cacheKey 缓存键
     */
    public void evictCache(String cacheName, String cacheKey) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(cacheKey);
            log.debug("Evicted cache: {}:{}", cacheName, cacheKey);
        }
    }

    /**
     * 清空指定缓存
     *
     * @param cacheName 缓存名称
     */
    public void clearCache(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared cache: {}", cacheName);
        }
    }

    /**
     * 清空所有特性开关缓存
     */
    public void clearAllCaches() {
        clearCache(FEATURE_STATE_CACHE);
        clearCache(VARIANT_CACHE);
        clearCache(GROUP_ASSIGNMENT_CACHE);
        clearCache(ROLLOUT_CACHE);
        log.info("Cleared all feature toggle caches");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public CacheStats getStats() {
        Cache stateCache = getCache(FEATURE_STATE_CACHE);
        Cache variantCache = getCache(VARIANT_CACHE);
        Cache groupCache = getCache(GROUP_ASSIGNMENT_CACHE);
        Cache rolloutCache = getCache(ROLLOUT_CACHE);

        return new CacheStats(
                getCacheStats(stateCache),
                getCacheStats(variantCache),
                getCacheStats(groupCache),
                getCacheStats(rolloutCache)
        );
    }

    /**
     * 获取单个缓存的统计信息
     */
    private CacheStatistics getCacheStats(Cache cache) {
        if (cache instanceof CaffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    ((CaffeineCache) cache).getNativeCache();

            return new CacheStatistics(
                    nativeCache.stats().hitCount(),
                    nativeCache.stats().missCount(),
                    nativeCache.stats().evictionCount(),
                    0L // Caffeine Cache不提供size()方法
            );
        }
        return new CacheStatistics(0, 0, 0, 0);
    }

    /**
     * 获取缓存实例
     */
    private Cache getCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            log.warn("Cache not found: {}", name);
        }
        return cache;
    }

    /**
     * 生成特性状态缓存键
     */
    private String generateStateCacheKey(String featureName, FeatureContext context) {
        StringBuilder keyBuilder = new StringBuilder("state:");
        keyBuilder.append(featureName);

        if (context != null) {
            if (context.getUserId() != null) {
                keyBuilder.append(":user:").append(context.getUserId());
            } else if (context.getTenantId() != null) {
                keyBuilder.append(":tenant:").append(context.getTenantId());
            } else if (context.getSessionId() != null) {
                keyBuilder.append(":session:").append(context.getSessionId());
            } else if (context.getIpAddress() != null) {
                keyBuilder.append(":ip:").append(context.getIpAddress());
            }
        }

        return keyBuilder.toString();
    }

    /**
     * 生成变体缓存键
     */
    private String generateVariantCacheKey(String featureName, FeatureContext context) {
        return "variant:" + featureName + ":" + generateContextKey(context);
    }

    /**
     * 生成分组分配缓存键
     */
    private String generateGroupCacheKey(String featureName, FeatureContext context) {
        return "group:" + featureName + ":" + generateContextKey(context);
    }

    /**
     * 生成渐进式发布缓存键
     */
    private String generateRolloutCacheKey(String featureName, FeatureContext context) {
        return "rollout:" + featureName + ":" + generateContextKey(context);
    }

    /**
     * 生成上下文键
     */
    private String generateContextKey(FeatureContext context) {
        if (context == null) {
            return "none";
        }

        if (context.getUserId() != null) {
            return "user:" + context.getUserId();
        } else if (context.getTenantId() != null) {
            return "tenant:" + context.getTenantId();
        } else if (context.getSessionId() != null) {
            return "session:" + context.getSessionId();
        } else if (context.getIpAddress() != null) {
            return "ip:" + context.getIpAddress();
        }

        return "anonymous";
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final CacheStatistics state;
        private final CacheStatistics variant;
        private final CacheStatistics group;
        private final CacheStatistics rollout;

        public CacheStats(CacheStatistics state, CacheStatistics variant,
                         CacheStatistics group, CacheStatistics rollout) {
            this.state = state;
            this.variant = variant;
            this.group = group;
            this.rollout = rollout;
        }

        public CacheStatistics getState() {
            return state;
        }

        public CacheStatistics getVariant() {
            return variant;
        }

        public CacheStatistics getGroup() {
            return group;
        }

        public CacheStatistics getRollout() {
            return rollout;
        }

        public double getHitRate() {
            long totalHits = state.hits + variant.hits + group.hits + rollout.hits;
            long totalRequests = totalHits + state.misses + variant.misses + group.misses + rollout.misses;
            return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{state=%s, variant=%s, group=%s, rollout=%s, hitRate=%.2f%%}",
                    state, variant, group, rollout, getHitRate() * 100);
        }
    }

    /**
     * 单个缓存统计信息
     */
    public static class CacheStatistics {
        public final long hits;
        public final long misses;
        public final long evictions;
        public final long size;

        public CacheStatistics(long hits, long misses, long evictions, long size) {
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.size = size;
        }

        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStatistics{hits=%d, misses=%d, evictions=%d, size=%d, hitRate=%.2f%%}",
                    hits, misses, evictions, size, getHitRate() * 100);
        }
    }
}
