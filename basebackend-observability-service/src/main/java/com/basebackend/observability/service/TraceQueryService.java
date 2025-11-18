package com.basebackend.observability.service;

import com.basebackend.observability.dto.TraceQueryRequest;

import java.util.List;
import java.util.Map;

/**
 * 追踪查询服务接口
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
public interface TraceQueryService {
    
    /**
     * 根据TraceId查询追踪详情
     */
    Map<String, Object> getTraceById(String traceId);
    
    /**
     * 搜索追踪
     */
    Map<String, Object> searchTraces(TraceQueryRequest request);
    
    /**
     * 获取服务列表
     */
    List<String> getServices();
    
    /**
     * 获取追踪统计
     */
    Map<String, Object> getTraceStats(String serviceName, int hours);
}
