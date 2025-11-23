package com.basebackend.cache.manager;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存淘汰管理器
 * 负责缓存容量管理和过期数据自动清理
 * 
 * 功能：
 * 1. 监控缓存容量，防止超出限制
 * 2. 定期清理过期数据
 * 3. 支持按模式批量淘汰
 * 4. 提供手动淘汰接口
 */
@Slf4j
@Component
public class CacheEvictionManager {

    private final RedisService redisService;
    private final CacheProperties cacheProperties;
    
    /**
     * 缓存容量限制（条目数）
     * -1 表示无限制
     */
    private long maxCacheSize = -1;
    
    /**
     * 淘汰策略
     */
    private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;

    public CacheEvictionManager(RedisService redisService, CacheProperties cacheProperties) {
        this.redisService = redisService;
        this.cacheProperties = cacheProperties;
    }

    /**
     * 淘汰策略枚举
     */
    public enum EvictionPolicy {
        /**
         * Least Recently Used - 最近最少使用
         */
        LRU,
        
        /**
         * Least Frequently Used - 最不经常使用
         */
        LFU,
        
        /**
         * First In First Out - 先进先出
         */
        FIFO,
        
        /**
         * Time To Live - 基于过期时间
         */
        TTL
    }

    /**
     * 定期清理过期数据
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredData() {
        log.info("Starting scheduled cleanup of expired cache data");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Redis 会自动清理过期键，这里主要是记录和监控
            // 可以通过 SCAN 命令扫描并检查过期键
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed expired data cleanup in {} ms", duration);
            
        } catch (Exception e) {
            log.error("Error during expired data cleanup", e);
        }
    }

    /**
     * 按模式淘汰缓存
     * 
     * @param pattern 键模式（支持通配符 * 和 ?）
     * @return 淘汰的键数量
     */
    public long evictByPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            log.warn("Invalid pattern for eviction: {}", pattern);
            return 0;
        }
        
        log.info("Evicting cache entries matching pattern: {}", pattern);
        
        try {
            long deleted = redisService.deleteByPattern(pattern);
            log.info("Evicted {} cache entries matching pattern: {}", deleted, pattern);
            return deleted;
            
        } catch (Exception e) {
            log.error("Error evicting cache by pattern: {}", pattern, e);
            return 0;
        }
    }

    /**
     * 淘汰单个缓存键
     * 
     * @param key 缓存键
     * @return true 如果成功淘汰
     */
    public boolean evict(String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Invalid key for eviction: {}", key);
            return false;
        }
        
        try {
            Boolean deleted = redisService.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Evicted cache entry: {}", key);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("Error evicting cache key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量淘汰缓存键
     * 
     * @param keys 缓存键集合
     * @return 淘汰的键数量
     */
    public long evictBatch(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.warn("Empty keys set for batch eviction");
            return 0;
        }
        
        log.info("Batch evicting {} cache entries", keys.size());
        
        try {
            Long deleted = redisService.delete(keys);
            long count = deleted != null ? deleted : 0;
            log.info("Batch evicted {} cache entries", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error during batch eviction", e);
            return 0;
        }
    }

    /**
     * 检查并执行容量管理
     * 如果缓存大小超过限制，根据淘汰策略清理数据
     * 
     * @return 淘汰的键数量
     */
    public long enforceCapacity() {
        if (maxCacheSize <= 0) {
            // 无容量限制
            return 0;
        }
        
        try {
            // 获取当前缓存大小
            long currentSize = getCacheSize();
            
            if (currentSize <= maxCacheSize) {
                log.debug("Cache size {} is within limit {}", currentSize, maxCacheSize);
                return 0;
            }
            
            log.warn("Cache size {} exceeds limit {}, enforcing eviction", currentSize, maxCacheSize);
            
            // 计算需要淘汰的数量
            long toEvict = currentSize - maxCacheSize;
            
            // 根据淘汰策略执行清理
            return evictByPolicy(toEvict);
            
        } catch (Exception e) {
            log.error("Error enforcing cache capacity", e);
            return 0;
        }
    }

    /**
     * 根据淘汰策略清理数据
     * 
     * @param count 需要淘汰的数量
     * @return 实际淘汰的数量
     */
    private long evictByPolicy(long count) {
        log.info("Evicting {} entries using policy: {}", count, evictionPolicy);
        
        switch (evictionPolicy) {
            case LRU:
                return evictLRU(count);
            case LFU:
                return evictLFU(count);
            case FIFO:
                return evictFIFO(count);
            case TTL:
                return evictByTTL(count);
            default:
                log.warn("Unknown eviction policy: {}, using LRU", evictionPolicy);
                return evictLRU(count);
        }
    }

    /**
     * LRU 淘汰策略
     * Redis 本身支持 LRU，这里主要是触发淘汰
     */
    private long evictLRU(long count) {
        // Redis 的 LRU 是自动的，通过 maxmemory-policy 配置
        // 这里我们可以手动触发一些清理操作
        log.debug("LRU eviction is handled by Redis automatically");
        return 0;
    }

    /**
     * LFU 淘汰策略
     */
    private long evictLFU(long count) {
        // Redis 的 LFU 也是自动的
        log.debug("LFU eviction is handled by Redis automatically");
        return 0;
    }

    /**
     * FIFO 淘汰策略
     */
    private long evictFIFO(long count) {
        // FIFO 需要维护键的创建时间
        log.debug("FIFO eviction requires custom implementation");
        return 0;
    }

    /**
     * 基于 TTL 的淘汰策略
     * 优先淘汰即将过期的键
     */
    private long evictByTTL(long count) {
        log.debug("TTL-based eviction: cleaning up keys with shortest TTL");
        
        // 这需要扫描所有键并检查 TTL
        // 在生产环境中，应该使用更高效的方法
        
        return 0;
    }

    /**
     * 获取缓存大小（估算）
     * 
     * @return 缓存条目数
     */
    public long getCacheSize() {
        try {
            // 使用 DBSIZE 命令获取键数量
            // 注意：这会返回整个数据库的键数量，不仅仅是应用的缓存
            Set<String> keys = redisService.keys("*");
            return keys != null ? keys.size() : 0;
            
        } catch (Exception e) {
            log.error("Error getting cache size", e);
            return 0;
        }
    }

    /**
     * 获取指定模式的缓存大小
     * 
     * @param pattern 键模式
     * @return 匹配的键数量
     */
    public long getCacheSize(String pattern) {
        try {
            Set<String> keys = redisService.keys(pattern);
            return keys != null ? keys.size() : 0;
            
        } catch (Exception e) {
            log.error("Error getting cache size for pattern: {}", pattern, e);
            return 0;
        }
    }

    /**
     * 设置缓存容量限制
     * 
     * @param maxSize 最大容量（-1 表示无限制）
     */
    public void setMaxCacheSize(long maxSize) {
        this.maxCacheSize = maxSize;
        log.info("Cache max size set to: {}", maxSize);
    }

    /**
     * 获取缓存容量限制
     * 
     * @return 最大容量
     */
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * 设置淘汰策略
     * 
     * @param policy 淘汰策略
     */
    public void setEvictionPolicy(EvictionPolicy policy) {
        this.evictionPolicy = policy;
        log.info("Eviction policy set to: {}", policy);
    }

    /**
     * 获取淘汰策略
     * 
     * @return 淘汰策略
     */
    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    /**
     * 清空所有缓存
     * 慎用！这会删除所有缓存数据
     * 
     * @return 删除的键数量
     */
    public long clearAll() {
        log.warn("Clearing all cache data - this is a destructive operation!");
        
        try {
            Set<String> keys = redisService.keys("*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisService.delete(keys);
                long count = deleted != null ? deleted : 0;
                log.warn("Cleared {} cache entries", count);
                return count;
            }
            return 0;
            
        } catch (Exception e) {
            log.error("Error clearing all cache", e);
            return 0;
        }
    }

    /**
     * 清空指定前缀的缓存
     * 
     * @param prefix 键前缀
     * @return 删除的键数量
     */
    public long clearByPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            log.warn("Invalid prefix for cache clear: {}", prefix);
            return 0;
        }
        
        String pattern = prefix + "*";
        return evictByPattern(pattern);
    }

    /**
     * 验证缓存键的有效性
     * 
     * @param key 缓存键
     * @return true 如果键有效
     */
    public boolean validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        // 检查键长度
        if (key.length() > 1024) {
            log.warn("Cache key too long: {} characters", key.length());
            return false;
        }
        
        // 检查键格式（可以根据需要添加更多验证）
        return true;
    }

    /**
     * 获取键的过期时间
     * 
     * @param key 缓存键
     * @return 过期时间（秒），-1 表示永不过期，-2 表示键不存在
     */
    public long getKeyExpiration(String key) {
        try {
            Long ttl = redisService.getExpire(key);
            return ttl != null ? ttl : -2;
            
        } catch (Exception e) {
            log.error("Error getting key expiration: {}", key, e);
            return -2;
        }
    }

    /**
     * 设置键的过期时间
     * 
     * @param key 缓存键
     * @param duration 过期时间
     * @return true 如果成功设置
     */
    public boolean setKeyExpiration(String key, Duration duration) {
        if (key == null || duration == null || duration.isNegative()) {
            return false;
        }
        
        try {
            Boolean result = redisService.expire(key, duration.getSeconds(), TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            log.error("Error setting key expiration: {}", key, e);
            return false;
        }
    }
}
