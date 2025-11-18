package com.basebackend.observability.service;

import com.basebackend.observability.dto.LogQueryRequest;

import java.util.List;
import java.util.Map;

/**
 * 日志查询服务接口
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
public interface LogQueryService {
    
    /**
     * 搜索日志
     */
    Map<String, Object> searchLogs(LogQueryRequest request);
    
    /**
     * 获取服务列表
     */
    List<String> getServices();
    
    /**
     * 获取日志级别列表
     */
    List<String> getLogLevels();
    
    /**
     * 获取日志统计
     */
    Map<String, Object> getLogStats(String serviceName, int hours);
    
    /**
     * 实时日志流
     */
    List<Map<String, Object>> tailLogs(String serviceName, int lines);
}
