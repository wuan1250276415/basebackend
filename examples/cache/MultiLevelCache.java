package com.basebackend.examples.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 多级缓存核心实现
 * 支持 L1 (Caffeine) + L2 (Redis) 两级缓存
 *
 * @param <T> 缓存值类型
 */
@Slf4j
@Component
public class MultiLevelCache<T> {

    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix;
    private final ConcurrentMap<String, AtomicLong> hitCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> missCounter = new ConcurrentHashMap<>();

    public MultiLevelCache(
            @Qualifier("userCacheL1") Cache<String, Object> l1Cache,
            RedisTemplate<String, Object> redisTemplate,
            MultiLevelCacheProperties properties) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.keyPrefix = properties.getL2().getKeyPrefix();
    }

    /**
     * 获取缓存
     *
     * @param key    缓存键
     * @param type   数据类型
     * @param loader 数据加载器
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public T get(String key, Class<T> type, Supplier<T> loader) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        String fullKey = buildKey(key);

        try {
            // 1. 尝试从 L1 缓存获取
            Object value = l1Cache.getIfPresent(fullKey);
            if (value != null) {
                recordHit(fullKey);
                log.debug("L1 缓存命中: {}", key);
                return (T) value;
            }

            // 2. 尝试从 L2 缓存获取
            value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                recordHit(fullKey);
                log.debug("L2 缓存命中: {}", key);
                // 同步到 L1 缓存
                l1Cache.put(fullKey, value);
                return (T) value;
            }

            // 3. 缓存未命中，加载数据
            recordMiss(fullKey);
            log.debug("缓存未命中，加载数据: {}", key);

            T loadedValue = loader.get();
            if (loadedValue != null) {
                // 写入 L2 缓存
                putToL2(fullKey, loadedValue);
                // 写入 L1 缓存
                putToL1(fullKey, loadedValue);
                log.debug("数据已缓存: {}", key);
            }

            return loadedValue;

        } catch (Exception e) {
            log.error("缓存获取异常: key={}", key, e);
            // 降级到直接加载数据
            return loader.get();
        }
    }

    /**
     * 批量获取缓存
     *
     * @param keys   缓存键列表
     * @param type   数据类型
     * @param loader 批量数据加载器
     * @return 缓存值 Map
     */
    public ConcurrentMap<String, T> getAll(Iterable<String> keys, Class<T> type, Supplier<ConcurrentMap<String, T>> loader) {
        ConcurrentMap<String, T> result = new ConcurrentHashMap<>();
        ConcurrentMap<String, T> needLoad = new ConcurrentHashMap<>();

        // 并行从缓存获取
        for (String key : keys) {
            if (key == null) continue;

            String fullKey = buildKey(key);

            // 先查 L1
            Object value = l1Cache.getIfPresent(fullKey);
            if (value != null) {
                recordHit(fullKey);
                result.put(key, (T) value);
                continue;
            }

            // 再查 L2
            value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                recordHit(fullKey);
                l1Cache.put(fullKey, value);
                result.put(key, (T) value);
                continue;
            }

            // 需要加载
            recordMiss(fullKey);
            needLoad.put(key, null);
        }

        // 批量加载数据
        if (!needLoad.isEmpty()) {
            ConcurrentMap<String, T> loadedData = loader.get();
            if (loadedData != null) {
                loadedData.forEach((key, value) -> {
                    if (value != null) {
                        result.put(key, value);
                        String fullKey = buildKey(key);
                        putToL2(fullKey, value);
                        putToL1(fullKey, value);
                    }
                });
            }
        }

        return result;
    }

    /**
     * 设置缓存
     *
     * @param key  缓存键
     * @param value 缓存值
     * @param ttl  过期时间
     */
    public void put(String key, T value, Duration ttl) {
        if (key == null || value == null) return;

        String fullKey = buildKey(key);
        putToL1(fullKey, value);
        putToL2(fullKey, value, ttl);
        log.debug("数据已写入多级缓存: {}", key);
    }

    /**
     * 设置缓存（使用默认 TTL）
     *
     * @param key    缓存键
     * @param value  缓存值
     */
    public void put(String key, T value) {
        put(key, value, Duration.ofHours(1));
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        if (key == null) return;

        String fullKey = buildKey(key);
        evictFromL1(fullKey);
        evictFromL2(fullKey);
        log.debug("缓存已删除: {}", key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键列表
     */
    public void evictAll(Iterable<String> keys) {
        for (String key : keys) {
            if (key != null) {
                evict(key);
            }
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        try {
            l1Cache.invalidateAll();
            // 注意: Redis 清空要谨慎，通常按前缀删除
            log.warn("L1 缓存已清空，L2 缓存需要手动清理");
        } catch (Exception e) {
            log.error("清空缓存失败", e);
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean contains(String key) {
        if (key == null) return false;

        String fullKey = buildKey(key);
        return l1Cache.getIfPresent(fullKey) != null ||
               Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    /**
     * 获取缓存大小
     *
     * @return L1 缓存大小
     */
    public long size() {
        return l1Cache.estimatedSize();
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    public CacheStatistics getStatistics() {
        var stats = l1Cache.stats();

        long totalHits = hitCounter.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();

        long totalMisses = missCounter.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();

        return CacheStatistics.builder()
            .l1Size(l1Cache.estimatedSize())
            .l1RequestCount(stats.requestCount())
            .l1HitCount(stats.hitCount())
            .l1HitRate(stats.hitRate())
            .l1MissCount(stats.missCount())
            .totalHits(totalHits)
            .totalMisses(totalMisses)
            .overallHitRate(totalHits + totalMisses > 0 ?
                (double) totalHits / (totalHits + totalMisses) : 0.0)
            .build();
    }

    // ========================================
    // 私有方法
    // ========================================

    private String buildKey(String key) {
        return keyPrefix + key;
    }

    private void putToL1(String fullKey, Object value) {
        try {
            l1Cache.put(fullKey, value);
        } catch (Exception e) {
            log.error("L1 缓存写入失败: {}", fullKey, e);
        }
    }

    private void putToL2(String fullKey, Object value) {
        putToL2(fullKey, value, Duration.ofHours(1));
    }

    private void putToL2(String fullKey, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(fullKey, value, ttl);
        } catch (Exception e) {
            log.error("L2 缓存写入失败: {}", fullKey, e);
        }
    }

    private void evictFromL1(String fullKey) {
        try {
            l1Cache.invalidate(fullKey);
        } catch (Exception e) {
            log.error("L1 缓存删除失败: {}", fullKey, e);
        }
    }

    private void evictFromL2(String fullKey) {
        try {
            redisTemplate.delete(fullKey);
        } catch (Exception e) {
            log.error("L2 缓存删除失败: {}", fullKey, e);
        }
    }

    private void recordHit(String key) {
        hitCounter.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    private void recordMiss(String key) {
        missCounter.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    // ========================================
    // 内部类
    // ========================================

    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        private final long l1Size;
        private final long l1RequestCount;
        private final long l1HitCount;
        private final double l1HitRate;
        private final long l1MissCount;
        private final long totalHits;
        private final long totalMisses;
        private final double overallHitRate;

        public CacheStatistics(Builder builder) {
            this.l1Size = builder.l1Size;
            this.l1RequestCount = builder.l1RequestCount;
            this.l1HitCount = builder.l1HitCount;
            this.l1HitRate = builder.l1HitRate;
            this.l1MissCount = builder.l1MissCount;
            this.totalHits = builder.totalHits;
            this.totalMisses = builder.totalMisses;
            this.overallHitRate = builder.overallHitRate;
        }

        public static Builder builder() {
            return new Builder();
        }

        @lombok.Data
        @lombok.Builder
        public static class Builder {
            private long l1Size;
            private long l1RequestCount;
            private long l1HitCount;
            private double l1HitRate;
            private long l1MissCount;
            private long totalHits;
            private long totalMisses;
            private double overallHitRate;
        }
    }
}
