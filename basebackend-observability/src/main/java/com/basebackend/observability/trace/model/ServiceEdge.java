package com.basebackend.observability.trace.model;

import lombok.Builder;
import lombok.Data;

/**
 * 服务调用边
 */
@Data
@Builder
public class ServiceEdge {
    
    /**
     * 源服务
     */
    private String source;
    
    /**
     * 目标服务
     */
    private String target;
    
    /**
     * 调用次数
     */
    private Long callCount;
    
    /**
     * 错误次数
     */
    private Long errorCount;
    
    /**
     * 平均耗时
     */
    private Double avgDuration;
    
    /**
     * 错误率
     */
    private Double errorRate;
    
    /**
     * 吞吐量 (QPS)
     */
    private Double qps;
}
