package com.basebackend.logging.statistics.cache;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计缓存
 *
 * 提供 LRU + TTL 的双重缓存策略：
 * - LRU 缓存：限制缓存大小，自动淘汰最久未使用项
 * - TTL 过期：设置缓存项的生存时间
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class StatisticsCache {

    private final Integer maxSize;
    private final Duration ttl;
    private final ConcurrentHashMap<String, CacheEntry> cache;

    public StatisticsCache(Integer maxSize, Duration ttl) {
        this.maxSize = maxSize;
        this.ttl = ttl;
        this.cache = new ConcurrentHashMap<>();
        log.info("统计缓存初始化: maxSize={}, ttl={}", maxSize, ttl);
    }

    /**
     * 获取缓存项
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> type) {
        if (key == null)
            return null;

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // 检查 TTL
        if (Instant.now().isAfter(entry.getExpiryTime())) {
            cache.remove(key);
            log.debug("缓存项已过期: {}", key);
            return null;
        }

        // 更新访问时间 (LRU)
        entry.setLastAccessed(Instant.now());

        @SuppressWarnings("unchecked")
        T value = (T) entry.getValue();
        return value;
    }

    /**
     * 放入缓存项
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void put(String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        // 检查缓存大小，必要时淘汰
        if (cache.size() >= maxSize) {
            evictLRU();
        }

        CacheEntry entry = new CacheEntry();
        entry.setValue(value);
        entry.setCreatedTime(Instant.now());
        entry.setLastAccessed(Instant.now());
        entry.setExpiryTime(Instant.now().plus(ttl));

        cache.put(key, entry);
        log.debug("缓存项已存储: {}", key);
    }

    /**
     * 删除缓存项
     *
     * @param key 缓存键
     */
    public void remove(String key) {
        if (cache.remove(key) != null) {
            log.debug("缓存项已删除: {}", key);
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
        log.info("缓存已清空");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public CacheStatistics getStatistics() {
        int size = cache.size();
        double hitRatio = calculateHitRatio();
        return CacheStatistics.builder()
                .size(size)
                .maxSize(maxSize)
                .hitRatio(hitRatio)
                .build();
    }

    // ==================== 私有方法 ====================

    private void evictLRU() {
        if (cache.isEmpty())
            return;

        Instant oldestAccess = Instant.now();
        String oldestKey = null;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            Instant lastAccessed = entry.getValue().getLastAccessed();
            if (lastAccessed.isBefore(oldestAccess)) {
                oldestAccess = lastAccessed;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            log.debug("LRU 缓存淘汰: {}", oldestKey);
        }
    }

    private double calculateHitRatio() {
        // 简化：基于缓存项数量和创建时间估算
        long now = System.currentTimeMillis();
        long activeCount = cache.values().stream()
                .mapToLong(entry -> {
                    long age = now - entry.getCreatedTime().toEpochMilli();
                    return age < ttl.toMillis() ? 1 : 0;
                })
                .sum();

        return cache.isEmpty() ? 0.0 : (double) activeCount / cache.size();
    }

    // ==================== 数据模型 ====================

    private static class CacheEntry {
        private Object value;
        private Instant createdTime;
        private Instant lastAccessed;
        private Instant expiryTime;

        // Getters and setters
        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Instant getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(Instant createdTime) {
            this.createdTime = createdTime;
        }

        public Instant getLastAccessed() {
            return lastAccessed;
        }

        public void setLastAccessed(Instant lastAccessed) {
            this.lastAccessed = lastAccessed;
        }

        public Instant getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(Instant expiryTime) {
            this.expiryTime = expiryTime;
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        private int size;
        private int maxSize;
        private double hitRatio;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CacheStatistics stats = new CacheStatistics();

            public Builder size(int size) {
                stats.size = size;
                return this;
            }

            public Builder maxSize(int maxSize) {
                stats.maxSize = maxSize;
                return this;
            }

            public Builder hitRatio(double hitRatio) {
                stats.hitRatio = hitRatio;
                return this;
            }

            public CacheStatistics build() {
                return stats;
            }
        }
    }
}
