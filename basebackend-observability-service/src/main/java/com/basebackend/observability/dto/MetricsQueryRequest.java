package com.basebackend.observability.dto;

import lombok.Data;

/**
 * 指标查询请求
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
public class MetricsQueryRequest {
    
    /**
     * 指标名称
     */
    private String metricName;
    
    /**
     * 开始时间（时间戳）
     */
    private Long startTime;
    
    /**
     * 结束时间（时间戳）
     */
    private Long endTime;
    
    /**
     * 标签过滤
     */
    private String tags;
    
    /**
     * 聚合方式：avg, sum, max, min
     */
    private String aggregation;
}
