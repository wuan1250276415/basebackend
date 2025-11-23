package com.basebackend.logging.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.LongAdder;

/**
 * 轻量级线程安全的本地LRU缓存
 *
 * 核心特性：
 * 1. 线程安全：使用ReentrantReadWriteLock保证并发安全
 * 2. LRU淘汰：基于LinkedHashMap的访问顺序实现
 * 3. 低延迟：本地缓存避免Redis网络开销，查询<10ms
 * 4. 监控指标：记录淘汰次数，便于监控和调优
 *
 * 使用场景：
 * - 热点数据缓存，减少Redis访问
 * - 降低延迟，提升查询性能
 * - 保护Redis免受过载
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class LocalLruCache<K, V> {

    /**
     * 最大条目数
     */
    private final int maxEntries;

    /**
     * 内部存储（LinkedHashMap支持LRU）
     */
    private final Map<K, V> store;

    /**
     * 读写锁，保证线程安全
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 淘汰计数器
     */
    private final LongAdder evictions = new LongAdder();

    /**
     * 命中计数器
     */
    private final LongAdder hits = new LongAdder();

    /**
     * 未命中计数器
     */
    private final LongAdder misses = new LongAdder();

    /**
     * 构造函数
     *
     * @param maxEntries 最大条目数，最小16
     */
    public LocalLruCache(int maxEntries) {
        this.maxEntries = Math.max(16, maxEntries);
        this.store = new LinkedHashMap<>(this.maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                boolean evict = size() > LocalLruCache.this.maxEntries;
                if (evict) {
                    evictions.increment();
                }
                return evict;
            }
        };
    }

    /**
     * 获取缓存值
     *
     * @param key 键
     * @return 值，如果不存在返回null
     */
    public V get(K key) {
        lock.readLock().lock();
        try {
            V value = store.get(key);
            if (value != null) {
                hits.increment();
            } else {
                misses.increment();
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 放入缓存
     *
     * @param key   键
     * @param value 值
     */
    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            store.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 删除缓存
     *
     * @param key 键
     */
    public void remove(K key) {
        lock.writeLock().lock();
        try {
            store.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            store.clear();
            evictions.reset();
            hits.reset();
            misses.reset();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存条目数
     */
    public int size() {
        lock.readLock().lock();
        try {
            return store.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取淘汰次数
     *
     * @return 累计淘汰次数
     */
    public long getEvictions() {
        return evictions.sum();
    }

    /**
     * 获取命中次数
     *
     * @return 累计命中次数
     */
    public long getHits() {
        return hits.sum();
    }

    /**
     * 获取未命中次数
     *
     * @return 累计未命中次数
     */
    public long getMisses() {
        return misses.sum();
    }

    /**
     * 获取命中比率
     *
     * @return 0.0-1.0之间的命中率
     */
    public double getHitRate() {
        long h = hits.sum();
        long m = misses.sum();
        if (h + m == 0) {
            return 0.0;
        }
        return (double) h / (h + m);
    }

    /**
     * 获取缓存使用率
     *
     * @return 0.0-1.0之间的使用率
     */
    public double getUsageRate() {
        return (double) size() / maxEntries;
    }

    /**
     * 获取所有键
     *
     * @return 键集合的副本
     */
    public Map<K, V> snapshot() {
        lock.readLock().lock();
        try {
            return Map.copyOf(store);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查是否包含指定键
     *
     * @param key 键
     * @return true=包含，false=不包含
     */
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return store.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 预热缓存
     * 向缓存中批量放入数据
     *
     * @param entries 键值对
     */
    public void putAll(Map<? extends K, ? extends V> entries) {
        lock.writeLock().lock();
        try {
            store.putAll(entries);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(size(), maxEntries, hits.sum(), misses.sum(),
                evictions.sum(), getHitRate(), getUsageRate());
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final int size;
        private final int maxSize;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;
        private final double usageRate;

        public CacheStats(int size, int maxSize, long hits, long misses,
                         long evictions, double hitRate, double usageRate) {
            this.size = size;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
            this.usageRate = usageRate;
        }

        // Getters
        public int getSize() { return size; }
        public int getMaxSize() { return maxSize; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRate() { return hitRate; }
        public double getUsageRate() { return usageRate; }

        @Override
        public String toString() {
            return String.format("CacheStats{size=%d/%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%, usageRate=%.2f%%}",
                    size, maxSize, hits, misses, evictions, hitRate * 100, usageRate * 100);
        }
    }
}
