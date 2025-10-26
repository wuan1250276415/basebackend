package com.basebackend.observability.trace.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Span节点模型
 */
@Data
@Builder
public class SpanNode {
    
    private String spanId;
    
    private String parentSpanId;
    
    private String serviceName;
    
    private String operationName;
    
    private Long startTime;
    
    private Long duration;
    
    private Map<String, Object> tags;
    
    private String status;
    
    private Boolean isError;
    
    private Boolean isBottleneck;
    
    private String errorMessage;
    
    /**
     * 子节点
     */
    private List<SpanNode> children;
}
