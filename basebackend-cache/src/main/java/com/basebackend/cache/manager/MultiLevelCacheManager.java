package com.basebackend.cache.manager;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.metrics.CacheStatistics;
import com.basebackend.cache.service.RedisService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存管理器
 * 实现本地缓存（Caffeine）+ Redis 的两级缓存架构
 * 
 * 查询顺序：本地缓存 -> Redis
 * 更新策略：同时更新本地缓存和 Redis
 * 淘汰策略：本地缓存使用 LRU/LFU，Redis 使用 TTL
 * 同步机制：通过 Redis Pub/Sub 实现缓存失效通知
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "basebackend.cache.multi-level", name = "enabled", havingValue = "true")
public class MultiLevelCacheManager {

    private final CacheProperties cacheProperties;
    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    
    /**
     * 本地缓存实例
     */
    private Cache<String, Object> localCache;
    
    /**
     * 缓存失效通知的 Redis 频道
     */
    private static final String CACHE_EVICTION_CHANNEL = "cache:eviction";
    
    /**
     * 统计信息
     */
    private long localHitCount = 0;
    private long localMissCount = 0;
    private long redisHitCount = 0;
    private long redisMissCount = 0;

    public MultiLevelCacheManager(
            CacheProperties cacheProperties,
            RedisService redisService,
            RedisTemplate<String, Object> redisTemplate,
            RedisMessageListenerContainer listenerContainer) {
        this.cacheProperties = cacheProperties;
        this.redisService = redisService;
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
    }

    /**
     * 初始化本地缓存和缓存失效监听器
     */
    @PostConstruct
    public void init() {
        CacheProperties.MultiLevel multiLevel = cacheProperties.getMultiLevel();
        
        log.info("Initializing MultiLevelCacheManager");
        log.info("Local cache max size: {}", multiLevel.getLocalMaxSize());
        log.info("Local cache TTL: {}", multiLevel.getLocalTtl());
        log.info("Eviction policy: {}", multiLevel.getEvictionPolicy());
        
        // 构建本地缓存
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(multiLevel.getLocalMaxSize())
                .recordStats(); // 启用统计
        
        // 根据淘汰策略配置过期时间
        switch (multiLevel.getEvictionPolicy().toUpperCase()) {
            case "LRU":
                // LRU: 基于访问时间淘汰
                caffeineBuilder.expireAfterAccess(multiLevel.getLocalTtl());
                log.info("Using LRU eviction policy (expireAfterAccess)");
                break;
            case "LFU":
                // LFU: Caffeine 默认使用 Window TinyLFU，基于频率淘汰
                caffeineBuilder.expireAfterWrite(multiLevel.getLocalTtl());
                log.info("Using LFU eviction policy (Caffeine default Window TinyLFU)");
                break;
            case "FIFO":
                // FIFO: 基于写入时间淘汰
                caffeineBuilder.expireAfterWrite(multiLevel.getLocalTtl());
                log.info("Using FIFO eviction policy (expireAfterWrite)");
                break;
            default:
                log.warn("Unknown eviction policy: {}, using default LRU", multiLevel.getEvictionPolicy());
                caffeineBuilder.expireAfterAccess(multiLevel.getLocalTtl());
        }
        
        this.localCache = caffeineBuilder.build();
        
        // 订阅缓存失效通知
        setupCacheEvictionListener();
        
        log.info("MultiLevelCacheManager initialized successfully");
    }

    /**
     * 设置缓存失效监听器
     * 监听 Redis Pub/Sub 消息，同步清除本地缓存
     */
    private void setupCacheEvictionListener() {
        try {
            listenerContainer.addMessageListener((message, pattern) -> {
                try {
                    String key = new String(message.getBody());
                    log.debug("Received cache eviction notification for key: {}", key);
                    
                    // 清除本地缓存
                    localCache.invalidate(key);
                    
                    log.debug("Local cache evicted for key: {}", key);
                } catch (Exception e) {
                    log.error("Error processing cache eviction message", e);
                }
            }, new ChannelTopic(CACHE_EVICTION_CHANNEL));
            
            log.info("Cache eviction listener setup successfully on channel: {}", CACHE_EVICTION_CHANNEL);
        } catch (Exception e) {
            log.error("Failed to setup cache eviction listener", e);
        }
    }

    /**
     * 获取缓存
     * 查询顺序：本地缓存 -> Redis
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值，如果不存在返回 null
     */
    public <T> T get(String key, Class<T> type) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        
        // 1. 先查询本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            localHitCount++;
            log.debug("Local cache hit for key: {}", key);
            return type.cast(localValue);
        }
        
        localMissCount++;
        log.debug("Local cache miss for key: {}", key);
        
        // 2. 本地缓存未命中，查询 Redis
        Object redisValue = redisService.get(key);
        if (redisValue != null) {
            redisHitCount++;
            log.debug("Redis cache hit for key: {}", key);
            
            // 3. 将 Redis 中的数据同步到本地缓存
            localCache.put(key, redisValue);
            log.debug("Synced value from Redis to local cache for key: {}", key);
            
            return type.cast(redisValue);
        }
        
        redisMissCount++;
        log.debug("Redis cache miss for key: {}", key);
        
        return null;
    }

    /**
     * 设置缓存
     * 同时更新本地缓存和 Redis
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    public void set(String key, Object value, Duration ttl) {
        if (key == null || key.trim().isEmpty() || value == null) {
            log.warn("Invalid cache key or value, skipping set operation");
            return;
        }
        
        // 1. 更新本地缓存
        localCache.put(key, value);
        log.debug("Updated local cache for key: {}", key);
        
        // 2. 更新 Redis
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            redisService.set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
            log.debug("Updated Redis cache for key: {} with TTL: {}", key, ttl);
        } else {
            redisService.set(key, value);
            log.debug("Updated Redis cache for key: {} without TTL", key);
        }
    }

    /**
     * 设置缓存（使用默认 TTL）
     * 
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        set(key, value, cacheProperties.getMultiLevel().getLocalTtl());
    }

    /**
     * 删除缓存
     * 同时清除本地缓存和 Redis，并发送失效通知
     * 
     * @param key 缓存键
     */
    public void evict(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        
        // 1. 清除本地缓存
        localCache.invalidate(key);
        log.debug("Evicted local cache for key: {}", key);
        
        // 2. 清除 Redis
        redisService.delete(key);
        log.debug("Evicted Redis cache for key: {}", key);
        
        // 3. 发送缓存失效通知（通知其他节点清除本地缓存）
        publishEvictionNotification(key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        log.info("Clearing all caches");
        
        // 1. 清空本地缓存
        localCache.invalidateAll();
        log.debug("Cleared local cache");
        
        // 2. 清空 Redis（注意：这会清空所有 Redis 数据，慎用）
        // 这里我们不实现全局清空，因为 Redis 可能被多个应用共享
        log.warn("Redis cache clear not implemented to prevent data loss");
    }

    /**
     * 发布缓存失效通知
     * 通过 Redis Pub/Sub 通知其他节点清除本地缓存
     * 
     * @param key 缓存键
     */
    private void publishEvictionNotification(String key) {
        try {
            redisTemplate.convertAndSend(CACHE_EVICTION_CHANNEL, key);
            log.debug("Published cache eviction notification for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to publish cache eviction notification for key: {}", key, e);
        }
    }

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public CacheStatistics getStatistics() {
        CacheStats caffeineStats = localCache.stats();
        
        CacheStatistics statistics = CacheStatistics.builder()
                .cacheName("multi-level")
                .hitCount(localHitCount + redisHitCount)
                .missCount(localMissCount + redisMissCount)
                .evictionCount(caffeineStats.evictionCount())
                .size(localCache.estimatedSize())
                .averageLoadTime((long) caffeineStats.averageLoadPenalty() / 1_000_000) // 转换为毫秒
                .lastAccessTime(Instant.now())
                .build();
        
        statistics.calculateHitRate();
        
        log.debug("Cache statistics - Local hits: {}, Local misses: {}, Redis hits: {}, Redis misses: {}, Hit rate: {}",
                localHitCount, localMissCount, redisHitCount, redisMissCount, statistics.getHitRate());
        
        return statistics;
    }

    /**
     * 获取本地缓存大小
     * 
     * @return 本地缓存条目数
     */
    public long getLocalCacheSize() {
        return localCache.estimatedSize();
    }

    /**
     * 获取本地缓存命中率
     * 
     * @return 本地缓存命中率
     */
    public double getLocalHitRate() {
        long total = localHitCount + localMissCount;
        return total > 0 ? (double) localHitCount / total : 0.0;
    }

    /**
     * 获取 Redis 缓存命中率
     * 
     * @return Redis 缓存命中率
     */
    public double getRedisHitRate() {
        long total = redisHitCount + redisMissCount;
        return total > 0 ? (double) redisHitCount / total : 0.0;
    }

    /**
     * 获取整体命中率
     * 
     * @return 整体命中率
     */
    public double getOverallHitRate() {
        long totalHits = localHitCount + redisHitCount;
        long totalMisses = localMissCount + redisMissCount;
        long total = totalHits + totalMisses;
        return total > 0 ? (double) totalHits / total : 0.0;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        localHitCount = 0;
        localMissCount = 0;
        redisHitCount = 0;
        redisMissCount = 0;
        log.info("Cache statistics reset");
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("Destroying MultiLevelCacheManager");
        if (localCache != null) {
            localCache.invalidateAll();
            localCache.cleanUp();
        }
        log.info("MultiLevelCacheManager destroyed");
    }
}
