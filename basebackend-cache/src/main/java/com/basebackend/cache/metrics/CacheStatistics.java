package com.basebackend.cache.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 缓存统计信息
 * 记录缓存的命中率、大小等指标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatistics {
    
    /**
     * 缓存名称
     */
    private String cacheName;
    
    /**
     * 命中次数
     */
    private long hitCount;
    
    /**
     * 未命中次数
     */
    private long missCount;
    
    /**
     * 总操作次数
     */
    private long totalCount;
    
    /**
     * 命中率
     */
    private double hitRate;
    
    /**
     * 淘汰次数
     */
    private long evictionCount;
    
    /**
     * 缓存大小（条目数）
     */
    private long size;
    
    /**
     * 平均加载时间（毫秒）
     */
    private long averageLoadTime;
    
    /**
     * 最后访问时间
     */
    private Instant lastAccessTime;
    
    /**
     * 计算命中率
     */
    public void calculateHitRate() {
        this.totalCount = this.hitCount + this.missCount;
        if (this.totalCount > 0) {
            this.hitRate = (double) this.hitCount / this.totalCount;
        } else {
            this.hitRate = 0.0;
        }
    }
}
