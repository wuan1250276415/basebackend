package com.basebackend.cache.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 缓存指标数据模型
 * 记录单次缓存操作的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheMetrics {
    
    /**
     * 缓存名称
     */
    private String cacheName;
    
    /**
     * 操作类型 (GET, SET, EVICT, etc.)
     */
    private OperationType operationType;
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 操作耗时（毫秒）
     */
    private long latencyMs;
    
    /**
     * 是否命中（仅对 GET 操作有效）
     */
    private Boolean hit;
    
    /**
     * 操作时间戳
     */
    private Instant timestamp;
    
    /**
     * 错误信息（如果操作失败）
     */
    private String errorMessage;
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        GET,
        SET,
        EVICT,
        CLEAR,
        MULTI_GET,
        MULTI_SET,
        DELETE_BY_PATTERN
    }
}
