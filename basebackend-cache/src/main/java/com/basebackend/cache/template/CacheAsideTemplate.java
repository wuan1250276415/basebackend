package com.basebackend.cache.template;

import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.metrics.CacheMetricsService;
import com.basebackend.cache.service.RedisService;
import com.basebackend.cache.util.BloomFilterUtil;
import com.basebackend.cache.util.RedissonLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Cache-Aside 模式模板
 * 
 * 实现标准的 Cache-Aside 模式：
 * 1. 查询时先查缓存，未命中则从数据源加载并缓存
 * 2. 更新时先更新数据源，然后删除缓存
 * 
 * 防护机制：
 * - 布隆过滤器防止缓存穿透
 * - 分布式锁防止缓存击穿
 */
@Slf4j
@Component
public class CacheAsideTemplate {

    private final RedisService redisService;
    private final MultiLevelCacheManager multiLevelCacheManager; // Can be null if multi-level cache is disabled
    private final RedissonLockUtil lockUtil;
    private final BloomFilterUtil bloomFilterUtil;
    private final CacheMetricsService metricsService;
    
    /**
     * 默认锁等待时间（秒）
     */
    private static final long DEFAULT_LOCK_WAIT_TIME = 3;
    
    /**
     * 默认锁持有时间（秒）
     */
    private static final long DEFAULT_LOCK_LEASE_TIME = 10;
    
    /**
     * 空值缓存时间（秒）- 用于防止缓存穿透
     */
    private static final long NULL_CACHE_TTL = 60;

    public CacheAsideTemplate(
            RedisService redisService,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager,
            RedissonLockUtil lockUtil,
            BloomFilterUtil bloomFilterUtil,
            CacheMetricsService metricsService) {
        this.redisService = redisService;
        this.multiLevelCacheManager = multiLevelCacheManager;
        this.lockUtil = lockUtil;
        this.bloomFilterUtil = bloomFilterUtil;
        this.metricsService = metricsService;
    }

    /**
     * 获取缓存数据，未命中时从数据源加载
     * 
     * @param key 缓存键
     * @param dataLoader 数据加载函数
     * @param ttl 缓存过期时间
     * @param type 返回值类型
     * @return 缓存值或从数据源加载的值
     */
    public <T> T get(String key, Supplier<T> dataLoader, Duration ttl, Class<T> type) {
        return get(key, dataLoader, ttl, type, false);
    }

    /**
     * 获取缓存数据，未命中时从数据源加载
     * 
     * @param key 缓存键
     * @param dataLoader 数据加载函数
     * @param ttl 缓存过期时间
     * @param type 返回值类型
     * @param useBloomFilter 是否使用布隆过滤器防止缓存穿透
     * @return 缓存值或从数据源加载的值
     */
    public <T> T get(String key, Supplier<T> dataLoader, Duration ttl, Class<T> type, boolean useBloomFilter) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 布隆过滤器检查（如果启用）
            if (useBloomFilter && !bloomFilterUtil.mightContain(key)) {
                log.debug("Bloom filter check failed for key: {}, data does not exist", key);
                metricsService.recordMiss("cache-aside");
                return null;
            }
            
            // 2. 查询缓存（多级缓存或单级 Redis）
            T cachedValue = getCachedValue(key, type);
            if (cachedValue != null) {
                metricsService.recordHit("cache-aside");
                metricsService.recordLatency("cache-aside-get", System.currentTimeMillis() - startTime);
                return cachedValue;
            }
            
            // 3. 缓存未命中，使用分布式锁防止缓存击穿
            String lockKey = "lock:cache-aside:" + key;
            boolean locked = lockUtil.tryLock(lockKey, DEFAULT_LOCK_WAIT_TIME, DEFAULT_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!locked) {
                log.warn("Failed to acquire lock for key: {}, returning null", key);
                metricsService.recordMiss("cache-aside");
                return null;
            }
            
            try {
                // 4. 双重检查：获取锁后再次查询缓存
                cachedValue = getCachedValue(key, type);
                if (cachedValue != null) {
                    log.debug("Cache hit after acquiring lock for key: {}", key);
                    metricsService.recordHit("cache-aside");
                    return cachedValue;
                }
                
                // 5. 从数据源加载数据
                log.debug("Loading data from source for key: {}", key);
                T loadedValue = dataLoader.get();
                
                // 6. 缓存数据
                if (loadedValue != null) {
                    setCachedValue(key, loadedValue, ttl);
                    
                    // 添加到布隆过滤器
                    if (useBloomFilter) {
                        bloomFilterUtil.add(key);
                    }
                    
                    log.debug("Loaded and cached data for key: {}", key);
                } else {
                    // 缓存空值防止缓存穿透
                    log.debug("Loaded null value for key: {}, caching empty marker", key);
                    setCachedValue(key, createNullMarker(), Duration.ofSeconds(NULL_CACHE_TTL));
                }
                
                metricsService.recordMiss("cache-aside");
                metricsService.recordLatency("cache-aside-load", System.currentTimeMillis() - startTime);
                
                return loadedValue;
                
            } finally {
                // 7. 释放锁
                lockUtil.unlock(lockKey);
            }
            
        } catch (Exception e) {
            log.error("Error in cache-aside get operation for key: {}", key, e);
            metricsService.recordLatency("cache-aside-error", System.currentTimeMillis() - startTime);
            
            // 发生异常时直接从数据源加载
            try {
                return dataLoader.get();
            } catch (Exception ex) {
                log.error("Error loading data from source for key: {}", key, ex);
                return null;
            }
        }
    }

    /**
     * 更新数据并删除缓存
     * 
     * @param key 缓存键
     * @param updater 数据更新函数
     * @return 更新后的值
     */
    public <T> T update(String key, Function<T, T> updater) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 执行更新操作
            T updatedValue = updater.apply(null);
            
            // 2. 删除缓存
            evictCache(key);
            
            log.debug("Updated data and evicted cache for key: {}", key);
            metricsService.recordLatency("cache-aside-update", System.currentTimeMillis() - startTime);
            
            return updatedValue;
            
        } catch (Exception e) {
            log.error("Error in cache-aside update operation for key: {}", key, e);
            metricsService.recordLatency("cache-aside-error", System.currentTimeMillis() - startTime);
            throw e;
        }
    }

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    public void evict(String key) {
        evictCache(key);
        log.debug("Evicted cache for key: {}", key);
    }

    /**
     * 从缓存获取值
     */
    private <T> T getCachedValue(String key, Class<T> type) {
        if (multiLevelCacheManager != null) {
            return multiLevelCacheManager.get(key, type);
        } else {
            Object value = redisService.get(key);
            if (value != null && isNullMarker(value)) {
                return null;
            }
            return value != null ? type.cast(value) : null;
        }
    }

    /**
     * 设置缓存值
     */
    private void setCachedValue(String key, Object value, Duration ttl) {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.set(key, value, ttl);
        } else {
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisService.set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
            } else {
                redisService.set(key, value);
            }
        }
    }

    /**
     * 删除缓存
     */
    private void evictCache(String key) {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.evict(key);
        } else {
            redisService.delete(key);
        }
    }

    /**
     * 创建空值标记
     */
    private Object createNullMarker() {
        return "NULL_MARKER";
    }

    /**
     * 检查是否为空值标记
     */
    private boolean isNullMarker(Object value) {
        return "NULL_MARKER".equals(value);
    }
}
