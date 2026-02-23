package com.basebackend.cache.admin.dto;

import com.basebackend.cache.metrics.CacheStatistics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存摘要信息 DTO
 * 用于缓存列表接口的响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheInfoDTO {

    private String name;
    private long size;
    private double hitRate;
    private long hitCount;
    private long missCount;
    private boolean available;

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

        return CacheInfoDTO.builder()
                .name(name)
                .size(size)
                .hitRate(hitRate)
                .hitCount(hitCount)
                .missCount(missCount)
                .available(true)
                .build();
    }

    public static CacheInfoDTO unavailable(String name) {
        return CacheInfoDTO.builder()
                .name(name)
                .size(-1)
                .hitRate(0.0)
                .hitCount(0)
                .missCount(0)
                .available(false)
                .build();
    }
}
