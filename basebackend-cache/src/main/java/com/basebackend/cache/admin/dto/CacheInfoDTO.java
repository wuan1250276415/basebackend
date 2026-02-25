package com.basebackend.cache.admin.dto;

import com.basebackend.cache.metrics.CacheStatistics;

/**
 * 缓存摘要信息 DTO
 * 用于缓存列表接口的响应
 */
public record CacheInfoDTO(
        String name,
        long size,
        double hitRate,
        long hitCount,
        long missCount,
        boolean available
) {

    public static CacheInfoDTO from(String name, long size, CacheStatistics stats) {
        double hitRate = 0.0;
        long hitCount = 0;
        long missCount = 0;

        if (stats != null) {
            hitCount = stats.getHitCount();
            missCount = stats.getMissCount();
            long total = hitCount + missCount;
            hitRate = total > 0 ? (double) hitCount / total : 0.0;
        }

        return new CacheInfoDTO(
                name,
                size,
                hitRate,
                hitCount,
                missCount,
                true
        );
    }

    public static CacheInfoDTO unavailable(String name) {
        return new CacheInfoDTO(
                name,
                -1,
                0.0,
                0,
                0,
                false
        );
    }
}
