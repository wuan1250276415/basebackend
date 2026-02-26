package com.basebackend.cache.admin.dto;

import com.basebackend.cache.metrics.CacheStatistics;

import java.util.Set;

/**
 * 缓存详细信息 DTO
 * 包含完整统计数据和采样键列表
 */
public record CacheDetailDTO(
        String name,
        long size,
        double hitRate,
        long hitCount,
        long missCount,
        long totalCount,
        long evictionCount,
        long averageLoadTime,
        Set<String> sampleKeys
) {

    public static CacheDetailDTO from(String name, long size, CacheStatistics stats, Set<String> sampleKeys) {
        long hitCount = 0;
        long missCount = 0;
        long totalCount = 0;
        long evictionCount = 0;
        long averageLoadTime = 0;
        double hitRate = 0.0;

        if (stats != null) {
            hitCount = stats.getHitCount();
            missCount = stats.getMissCount();
            totalCount = stats.getTotalCount();
            evictionCount = stats.getEvictionCount();
            averageLoadTime = stats.getAverageLoadTime();
            hitRate = stats.getHitRate();
        }

        return new CacheDetailDTO(
                name,
                size,
                hitRate,
                hitCount,
                missCount,
                totalCount,
                evictionCount,
                averageLoadTime,
                sampleKeys
        );
    }
}
