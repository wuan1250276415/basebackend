package com.basebackend.logging.cache;

import java.util.concurrent.atomic.LongAdder;

/**
 * 热点日志缓存监控指标
 *
 * 使用LongAdder保证高并发场景下的性能。
 * 提供命中、未命中、淘汰、预热等关键指标。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class HotLogCacheMetrics {

    /**
     * 命中次数
     */
    private final LongAdder hits = new LongAdder();

    /**
     * 未命中次数
     */
    private final LongAdder misses = new LongAdder();

    /**
     * 淘汰次数
     */
    private final LongAdder evictions = new LongAdder();

    /**
     * 预热次数
     */
    private final LongAdder preload = new LongAdder();

    /**
     * 缓存项数
     */
    private final LongAdder cacheSize = new LongAdder();

    /**
     * 记录命中
     */
    public void hit() {
        hits.increment();
    }

    /**
     * 记录未命中
     */
    public void miss() {
        misses.increment();
    }

    /**
     * 记录淘汰
     */
    public void evict() {
        evictions.increment();
    }

    /**
     * 记录预热
     */
    public void preload() {
        preload.increment();
    }

    /**
     * 记录缓存大小变更
     *
     * @param delta 变化量（正数=增加，负数=减少）
     */
    public void updateSize(int delta) {
        cacheSize.add(delta);
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
     * 获取淘汰次数
     *
     * @return 累计淘汰次数
     */
    public long getEvictions() {
        return evictions.sum();
    }

    /**
     * 获取预热次数
     *
     * @return 累计预热次数
     */
    public long getPreload() {
        return preload.sum();
    }

    /**
     * 获取当前缓存项数
     *
     * @return 当前缓存项数
     */
    public long getCacheSize() {
        return cacheSize.sum();
    }

    /**
     * 获取总请求数
     *
     * @return 命中 + 未命中
     */
    public long getTotalRequests() {
        return hits.sum() + misses.sum();
    }

    /**
     * 获取命中率
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
     * 获取命中率百分比
     *
     * @return 0-100之间的命中率
     */
    public double getHitRatePercentage() {
        return getHitRate() * 100.0;
    }

    /**
     * 获取未命中率
     *
     * @return 0.0-1.0之间的未命中率
     */
    public double getMissRate() {
        long h = hits.sum();
        long m = misses.sum();
        if (h + m == 0) {
            return 0.0;
        }
        return (double) m / (h + m);
    }

    /**
     * 获取淘汰率
     *
     * @return 0.0-1.0之间的淘汰率（相对于总请求）
     */
    public double getEvictionRate() {
        long total = getTotalRequests();
        if (total == 0) {
            return 0.0;
        }
        return (double) evictions.sum() / total;
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        hits.reset();
        misses.reset();
        evictions.reset();
        preload.reset();
        cacheSize.reset();
    }

    /**
     * 获取性能评级
     *
     * @return 性能评级（EXCELLENT/GOOD/FAIR/POOR）
     */
    public PerformanceGrade getPerformanceGrade() {
        double hitRate = getHitRate();
        if (hitRate >= 0.9) {
            return PerformanceGrade.EXCELLENT;
        } else if (hitRate >= 0.7) {
            return PerformanceGrade.GOOD;
        } else if (hitRate >= 0.5) {
            return PerformanceGrade.FAIR;
        } else {
            return PerformanceGrade.POOR;
        }
    }

    /**
     * 性能等级枚举
     */
    public enum PerformanceGrade {
        /**
         * 优秀：命中率 >= 90%
         */
        EXCELLENT("优秀", "命中率 ≥ 90%，缓存效果极佳"),

        /**
         * 良好：命中率 70-90%
         */
        GOOD("良好", "命中率 70-90%，缓存效果良好"),

        /**
         * 一般：命中率 50-70%
         */
        FAIR("一般", "命中率 50-70%，缓存效果一般，建议优化"),

        /**
         * 较差：命中率 < 50%
         */
        POOR("较差", "命中率 < 50%，缓存效果较差，需要优化");

        private final String label;
        private final String description;

        PerformanceGrade(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取统计摘要
     *
     * @return 统计摘要字符串
     */
    public String getSummary() {
        return String.format(
                "热点缓存统计 - 命中: %d, 未命中: %d, 淘汰: %d, 预热: %d, 命中率: %.2f%%, 性能等级: %s",
                hits.sum(), misses.sum(), evictions.sum(), preload.sum(),
                getHitRatePercentage(), getPerformanceGrade().getLabel());
    }

    /**
     * 转换为Map格式
     *
     * @return 指标Map
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("hits", getHits());
        map.put("misses", getMisses());
        map.put("evictions", getEvictions());
        map.put("preload", getPreload());
        map.put("cacheSize", getCacheSize());
        map.put("totalRequests", getTotalRequests());
        map.put("hitRate", getHitRate());
        map.put("hitRatePercentage", getHitRatePercentage());
        map.put("missRate", getMissRate());
        map.put("evictionRate", getEvictionRate());
        map.put("performanceGrade", getPerformanceGrade().name());
        return map;
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
