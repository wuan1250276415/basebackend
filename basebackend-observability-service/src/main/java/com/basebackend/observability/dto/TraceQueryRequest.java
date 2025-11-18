package com.basebackend.observability.dto;

import lombok.Data;

/**
 * 追踪查询请求
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
public class TraceQueryRequest {
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 操作名称
     */
    private String operationName;
    
    /**
     * 开始时间（时间戳）
     */
    private Long startTime;
    
    /**
     * 结束时间（时间戳）
     */
    private Long endTime;
    
    /**
     * 最小持续时间（毫秒）
     */
    private Long minDuration;
    
    /**
     * 最大持续时间（毫秒）
     */
    private Long maxDuration;
    
    /**
     * 标签过滤
     */
    private String tags;
    
    /**
     * 限制结果数量
     */
    private Integer limit = 100;
}
