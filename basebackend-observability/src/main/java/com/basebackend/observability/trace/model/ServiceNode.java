package com.basebackend.observability.trace.model;

import lombok.Builder;
import lombok.Data;

/**
 * 服务节点
 */
@Data
@Builder
public class ServiceNode {
    
    /**
     * 服务名
     */
    private String name;
    
    /**
     * 调用次数
     */
    private Long callCount;
    
    /**
     * 错误次数
     */
    private Long errorCount;
    
    /**
     * 平均响应时间
     */
    private Double avgDuration;
    
    /**
     * P95响应时间
     */
    private Double p95Duration;
    
    /**
     * 错误率
     */
    private Double errorRate;
    
    /**
     * 健康分数 (0-100)
     */
    private Integer healthScore;
    
    /**
     * 类型（INTERNAL/EXTERNAL）
     */
    private String type;
}
