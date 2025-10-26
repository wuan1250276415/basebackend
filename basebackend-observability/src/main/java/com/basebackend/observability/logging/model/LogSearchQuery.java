package com.basebackend.observability.logging.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志搜索查询条件
 */
@Data
@Builder(toBuilder = true)
public class LogSearchQuery {
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 服务名列表
     */
    private List<String> services;
    
    /**
     * 日志级别列表
     */
    private List<String> levels;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * TraceId
     */
    private String traceId;
    
    /**
     * 异常类名
     */
    private String exceptionClass;
    
    /**
     * 分页 - 起始位置
     */
    private Integer from;
    
    /**
     * 分页 - 数量
     */
    private Integer size;
    
    /**
     * 排序字段
     */
    private String sortField;
    
    /**
     * 排序方向
     */
    private String sortOrder;
    
    /**
     * 是否简单查询（用于引擎选择）
     */
    public boolean isSimpleQuery() {
        return keyword == null || keyword.length() < 50;
    }
    
    /**
     * 获取时间范围（毫秒）
     */
    public long getTimeRange() {
        if (startTime == null || endTime == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
}
