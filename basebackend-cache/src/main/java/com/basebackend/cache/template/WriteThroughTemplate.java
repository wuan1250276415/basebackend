package com.basebackend.cache.template;

import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.metrics.CacheMetricsService;
import com.basebackend.cache.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Write-Through 模式模板
 * 
 * 实现 Write-Through 缓存模式：
 * 1. 写操作同步更新缓存和数据源
 * 2. 缓存作为数据源的代理，保证数据一致性
 * 3. 写操作会阻塞直到数据源更新完成
 * 
 * 适用场景：
 * - 需要强一致性的场景
 * - 写操作不频繁的场景
 * - 可以接受写延迟的场景
 */
@Slf4j
@Component
public class WriteThroughTemplate {

    private final RedisService redisService;
    private final MultiLevelCacheManager multiLevelCacheManager; // Can be null if multi-level cache is disabled
    private final CacheMetricsService metricsService;

    public WriteThroughTemplate(
            RedisService redisService,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager,
            CacheMetricsService metricsService) {
        this.redisService = redisService;
        this.multiLevelCacheManager = multiLevelCacheManager;
        this.metricsService = metricsService;
    }

    /**
     * 同步写入缓存和数据源
     * 
     * @param key 缓存键
     * @param value 要写入的值
     * @param dataPersister 数据持久化函数
     * @param ttl 缓存过期时间
     */
    public <T> void set(String key, T value, Consumer<T> dataPersister, Duration ttl) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 先更新数据源
            log.debug("Persisting data to source for key: {}", key);
            dataPersister.accept(value);
            
            // 2. 数据源更新成功后，更新缓存
            log.debug("Updating cache for key: {}", key);
            setCachedValue(key, value, ttl);
            
            log.debug("Write-through completed for key: {}", key);
            metricsService.recordLatency("write-through-set", System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Error in write-through set operation for key: {}", key, e);
            metricsService.recordLatency("write-through-error", System.currentTimeMillis() - startTime);
            
            // 如果数据源更新失败，不更新缓存，保证一致性
            throw new RuntimeException("Write-through failed for key: " + key, e);
        }
    }

    /**
     * 同步写入缓存和数据源（使用默认 TTL）
     * 
     * @param key 缓存键
     * @param value 要写入的值
     * @param dataPersister 数据持久化函数
     */
    public <T> void set(String key, T value, Consumer<T> dataPersister) {
        set(key, value, dataPersister, null);
    }

    /**
     * 同步删除缓存和数据源
     * 
     * @param key 缓存键
     * @param dataDeleter 数据删除函数
     */
    public void delete(String key, Runnable dataDeleter) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 先删除数据源
            log.debug("Deleting data from source for key: {}", key);
            dataDeleter.run();
            
            // 2. 数据源删除成功后，删除缓存
            log.debug("Deleting cache for key: {}", key);
            evictCache(key);
            
            log.debug("Write-through delete completed for key: {}", key);
            metricsService.recordLatency("write-through-delete", System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Error in write-through delete operation for key: {}", key, e);
            metricsService.recordLatency("write-through-error", System.currentTimeMillis() - startTime);
            
            // 如果数据源删除失败，不删除缓存，保证一致性
            throw new RuntimeException("Write-through delete failed for key: " + key, e);
        }
    }

    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> type) {
        long startTime = System.currentTimeMillis();
        
        try {
            T value = getCachedValue(key, type);
            
            if (value != null) {
                metricsService.recordHit("write-through");
            } else {
                metricsService.recordMiss("write-through");
            }
            
            metricsService.recordLatency("write-through-get", System.currentTimeMillis() - startTime);
            return value;
            
        } catch (Exception e) {
            log.error("Error in write-through get operation for key: {}", key, e);
            metricsService.recordLatency("write-through-error", System.currentTimeMillis() - startTime);
            return null;
        }
    }

    /**
     * 从缓存获取值
     */
    private <T> T getCachedValue(String key, Class<T> type) {
        if (multiLevelCacheManager != null) {
            return multiLevelCacheManager.get(key, type);
        } else {
            Object value = redisService.get(key);
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
}
