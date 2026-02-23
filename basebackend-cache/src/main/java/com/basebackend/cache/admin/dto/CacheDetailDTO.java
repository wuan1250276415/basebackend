package com.basebackend.cache.admin.dto;

import com.basebackend.cache.metrics.CacheStatistics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 缓存详细信息 DTO
 * 包含完整统计数据和采样键列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDetailDTO {

    private String name;
    private long size;
    private double hitRate;
    private long hitCount;
    private long missCount;
    private long totalCount;
    private long evictionCount;
    private long averageLoadTime;
    private Set<String> sampleKeys;

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

        return CacheDetailDTO.builder()
                .name(name)
                .size(size)
                .hitRate(hitRate)
                .hitCount(hitCount)
                .missCount(missCount)
                .totalCount(totalCount)
                .evictionCount(evictionCount)
                .averageLoadTime(averageLoadTime)
                .sampleKeys(sampleKeys)
                .build();
    }
}
