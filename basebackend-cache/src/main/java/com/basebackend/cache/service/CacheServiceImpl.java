package com.basebackend.cache.service;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.CacheEvictionManager;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.metrics.CacheMetricsService;
import com.basebackend.cache.metrics.CacheStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 统一缓存服务实现
 * 整合 RedisService、MultiLevelCacheManager、CacheEvictionManager 和 CacheMetricsService
 * 提供完整的缓存操作 API
 */
@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    private final RedisService redisService;
    private final CacheProperties cacheProperties;
    private final CacheMetricsService metricsService;
    private final CacheEvictionManager evictionManager;
    
    @Autowired(required = false)
    private MultiLevelCacheManager multiLevelCacheManager;
    
    /**
     * 缓存名称验证正则表达式
     * 允许字母、数字、下划线、连字符
     */
    private static final Pattern CACHE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    /**
     * 默认 TTL
     */
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    public CacheServiceImpl(
            RedisService redisService,
            CacheProperties cacheProperties,
            CacheMetricsService metricsService,
            CacheEvictionManager evictionManager) {
        this.redisService = redisService;
        this.cacheProperties = cacheProperties;
        this.metricsService = metricsService;
        this.evictionManager = evictionManager;
    }

    // ========== 基本缓存操作 ==========

    @Override
    public <T> T get(String key, Class<T> type) {
        if (!validateKey(key)) {
            return null;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            T value;
            
            // 如果启用多级缓存，使用多级缓存管理器
            if (isMultiLevelEnabled()) {
                value = multiLevelCacheManager.get(key, type);
            } else {
                Object obj = redisService.get(key);
                value = obj != null ? type.cast(obj) : null;
            }
            
            // 记录指标
            long latency = System.currentTimeMillis() - startTime;
            if (value != null) {
                metricsService.recordHit(getCacheName(key));
            } else {
                metricsService.recordMiss(getCacheName(key));
            }
            metricsService.recordLatency(getCacheName(key), "GET", latency);
            
            return value;
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordLatency(getCacheName(key), "GET", latency, false, e.getMessage());
            log.error("Error getting cache: {}", key, e);
            return null;
        }
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        if (!validateKey(key) || value == null) {
            log.warn("Invalid key or value for cache set");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 如果启用多级缓存，使用多级缓存管理器
            if (isMultiLevelEnabled()) {
                multiLevelCacheManager.set(key, value, ttl);
            } else {
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    redisService.set(key, value, ttl.getSeconds(), TimeUnit.SECONDS);
                } else {
                    redisService.set(key, value);
                }
            }
            
            // 记录指标
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordSet(getCacheName(key), latency, true);
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordSet(getCacheName(key), latency, false);
            log.error("Error setting cache: {}", key, e);
        }
    }

    @Override
    public boolean delete(String key) {
        if (!validateKey(key)) {
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 如果启用多级缓存，使用多级缓存管理器
            if (isMultiLevelEnabled()) {
                multiLevelCacheManager.evict(key);
            } else {
                Boolean result = redisService.delete(key);
                if (!Boolean.TRUE.equals(result)) {
                    return false;
                }
            }
            
            // 记录指标
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordEviction(getCacheName(key), latency, true);
            
            return true;
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordEviction(getCacheName(key), latency, false);
            log.error("Error deleting cache: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        if (!validateKey(key)) {
            return false;
        }
        
        try {
            Boolean result = redisService.hasKey(key);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            log.error("Error checking cache existence: {}", key, e);
            return false;
        }
    }

    // ========== Cache-Aside 模式 ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Supplier<T> loader, Duration ttl) {
        if (!validateKey(key) || loader == null) {
            return null;
        }
        
        // 先尝试从缓存获取
        T value = get(key, (Class<T>) Object.class);
        
        if (value != null) {
            return value;
        }
        
        // 缓存未命中，从数据源加载
        log.debug("Cache miss for key: {}, loading from data source", key);
        
        try {
            value = loader.get();
            
            if (value != null) {
                // 将加载的数据放入缓存
                set(key, value, ttl);
                log.debug("Loaded and cached value for key: {}", key);
            }
            
            return value;
            
        } catch (Exception e) {
            log.error("Error loading data for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> T getOrLoad(String key, Supplier<T> loader) {
        return getOrLoad(key, loader, DEFAULT_TTL);
    }

    // ========== 批量操作 ==========

    @Override
    public <T> Map<String, T> multiGet(Set<String> keys, Class<T> type) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        
        try {
            return redisService.multiGet(keys);
            
        } catch (Exception e) {
            log.error("Error in multiGet", e);
            return Map.of();
        }
    }

    @Override
    public void multiSet(Map<String, Object> entries, Duration ttl) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        
        try {
            redisService.multiSet(entries, ttl);
            
        } catch (Exception e) {
            log.error("Error in multiSet", e);
        }
    }

    @Override
    public void multiSet(Map<String, Object> entries) {
        multiSet(entries, DEFAULT_TTL);
    }

    @Override
    public long multiDelete(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        try {
            Long deleted = redisService.delete(keys);
            return deleted != null ? deleted : 0;
            
        } catch (Exception e) {
            log.error("Error in multiDelete", e);
            return 0;
        }
    }

    // ========== 模式匹配操作 ==========

    @Override
    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 0;
        }
        
        try {
            return redisService.deleteByPattern(pattern);
            
        } catch (Exception e) {
            log.error("Error deleting by pattern: {}", pattern, e);
            return 0;
        }
    }

    @Override
    public Set<String> keys(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return Set.of();
        }

        try {
            return redisService.scan(pattern);

        } catch (Exception e) {
            log.error("Error getting keys by pattern: {}", pattern, e);
            return Set.of();
        }
    }

    // ========== 缓存生命周期管理 ==========

    @Override
    public boolean createCache(String cacheName) {
        if (!validateCacheName(cacheName)) {
            log.warn("Invalid cache name: {}", cacheName);
            return false;
        }
        
        log.info("Creating cache: {}", cacheName);
        
        // 缓存创建逻辑（如果需要的话）
        // Redis 不需要显式创建缓存，键会自动创建
        
        return true;
    }

    @Override
    public boolean validateCacheName(String cacheName) {
        if (cacheName == null || cacheName.trim().isEmpty()) {
            return false;
        }
        
        // 检查长度
        if (cacheName.length() > 100) {
            log.warn("Cache name too long: {}", cacheName.length());
            return false;
        }
        
        // 检查格式
        if (!CACHE_NAME_PATTERN.matcher(cacheName).matches()) {
            log.warn("Cache name contains invalid characters: {}", cacheName);
            return false;
        }
        
        return true;
    }

    @Override
    public long clearCache(String cacheName) {
        if (!validateCacheName(cacheName)) {
            return 0;
        }
        
        log.info("Clearing cache: {}", cacheName);
        
        String pattern = buildCachePattern(cacheName);
        return evictionManager.evictByPattern(pattern);
    }

    @Override
    public long clearAllCaches() {
        return clearAllCaches(false);
    }

    /**
     * 清空所有缓存
     * 安全增强：需要显式确认才能执行
     *
     * @param confirmed 是否确认执行此操作（true表示确认，false表示取消）
     * @return 删除的键数量
     */
    public long clearAllCaches(boolean confirmed) {
        if (!confirmed) {
            log.warn("clearAllCaches() called without confirmation - operation cancelled");
            return 0;
        }

        log.warn("Clearing all caches - this is a destructive operation!");
        return evictionManager.clearAll(confirmed);
    }

    // ========== 缓存统计和监控 ==========

    @Override
    public CacheStatistics getStatistics(String cacheName) {
        if (isMultiLevelEnabled()) {
            return multiLevelCacheManager.getStatistics();
        }
        
        return metricsService.getStatistics(cacheName);
    }

    @Override
    public long getCacheSize(String cacheName) {
        String pattern = buildCachePattern(cacheName);
        return evictionManager.getCacheSize(pattern);
    }

    @Override
    public Set<String> getAllCacheNames() {
        return metricsService.getAllCacheNames();
    }

    @Override
    public void resetStatistics(String cacheName) {
        metricsService.resetStatistics(cacheName);
        
        if (isMultiLevelEnabled()) {
            multiLevelCacheManager.resetStatistics();
        }
    }

    // ========== 过期时间管理 ==========

    @Override
    public long getExpiration(String key) {
        if (!validateKey(key)) {
            return -2;
        }
        
        return evictionManager.getKeyExpiration(key);
    }

    @Override
    public boolean setExpiration(String key, Duration duration) {
        if (!validateKey(key) || duration == null) {
            return false;
        }
        
        return evictionManager.setKeyExpiration(key, duration);
    }

    @Override
    public boolean removeExpiration(String key) {
        if (!validateKey(key)) {
            return false;
        }
        
        try {
            // 设置为永不过期（-1）
            Boolean result = redisService.expire(key, -1, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            log.error("Error removing expiration for key: {}", key, e);
            return false;
        }
    }

    // ========== 容量管理 ==========

    @Override
    public long enforceCapacity() {
        return evictionManager.enforceCapacity();
    }

    @Override
    public void setMaxCacheSize(long maxSize) {
        evictionManager.setMaxCacheSize(maxSize);
    }

    @Override
    public long getMaxCacheSize() {
        return evictionManager.getMaxCacheSize();
    }

    // ========== 辅助方法 ==========

    /**
     * 验证缓存键
     */
    private boolean validateKey(String key) {
        return evictionManager.validateKey(key);
    }

    /**
     * 从键中提取缓存名称
     */
    private String getCacheName(String key) {
        if (key == null) {
            return "default";
        }
        
        // 假设键格式为 "cacheName:actualKey"
        int separatorIndex = key.indexOf(':');
        if (separatorIndex > 0) {
            return key.substring(0, separatorIndex);
        }
        
        return "default";
    }

    /**
     * 构建缓存模式
     */
    private String buildCachePattern(String cacheName) {
        String prefix = cacheProperties.getKey().getPrefix();
        String separator = cacheProperties.getKey().getSeparator();
        
        return prefix + separator + cacheName + separator + "*";
    }

    /**
     * 检查是否启用多级缓存
     */
    private boolean isMultiLevelEnabled() {
        return cacheProperties.getMultiLevel().isEnabled() && multiLevelCacheManager != null;
    }
}
