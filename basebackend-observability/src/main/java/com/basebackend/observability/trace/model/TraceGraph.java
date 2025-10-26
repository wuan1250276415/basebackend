package com.basebackend.observability.trace.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 追踪调用图模型
 */
@Data
@Builder
public class TraceGraph {
    
    /**
     * 根Span节点
     */
    private SpanNode rootSpan;
    
    /**
     * 关键路径（耗时最长的调用链）
     */
    private List<String> criticalPath;
    
    /**
     * 总耗时
     */
    private Long totalDuration;
    
    /**
     * Span数量
     */
    private Integer spanCount;
    
    /**
     * 服务数量
     */
    private Integer serviceCount;
    
    /**
     * 错误路径
     */
    private List<String> errorPath;
    
    /**
     * 根因Span
     */
    private SpanNode rootCause;
}
