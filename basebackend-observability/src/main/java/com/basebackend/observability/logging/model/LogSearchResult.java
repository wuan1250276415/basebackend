package com.basebackend.observability.logging.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 日志搜索结果
 */
@Data
@Builder
public class LogSearchResult {
    
    /**
     * 日志列表
     */
    private List<LogEntry> logs;
    
    /**
     * 总数
     */
    private Long total;
    
    /**
     * 聚合结果
     */
    private Map<String, Object> aggregations;
    
    /**
     * 耗时（毫秒）
     */
    private Long took;
}
