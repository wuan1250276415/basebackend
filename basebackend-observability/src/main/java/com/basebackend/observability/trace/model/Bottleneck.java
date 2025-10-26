package com.basebackend.observability.trace.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 性能瓶颈模型
 */
@Data
@Builder
public class Bottleneck {
    
    /**
     * 瓶颈类型
     */
    private BottleneckType type;
    
    /**
     * 严重程度
     */
    private Severity severity;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 涉及的SpanId列表
     */
    private List<String> spanIds;
    
    /**
     * SQL查询（N+1问题）
     */
    private String sqlQuery;
    
    /**
     * 查询次数
     */
    private Integer queryCount;
    
    /**
     * 总耗时
     */
    private Long totalDuration;
    
    /**
     * 优化建议
     */
    private String suggestion;
    
    /**
     * 瓶颈位置（服务名.方法名）
     */
    private String location;
}
