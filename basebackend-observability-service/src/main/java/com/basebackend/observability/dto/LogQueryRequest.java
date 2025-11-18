package com.basebackend.observability.dto;

import lombok.Data;

/**
 * 日志查询请求
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
public class LogQueryRequest {
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 日志级别：DEBUG, INFO, WARN, ERROR
     */
    private String level;
    
    /**
     * 关键词搜索
     */
    private String keyword;
    
    /**
     * 开始时间（时间戳）
     */
    private Long startTime;
    
    /**
     * 结束时间（时间戳）
     */
    private Long endTime;
    
    /**
     * 限制结果数量
     */
    private Integer limit = 100;
    
    /**
     * 排序方式：asc, desc
     */
    private String sort = "desc";
}
