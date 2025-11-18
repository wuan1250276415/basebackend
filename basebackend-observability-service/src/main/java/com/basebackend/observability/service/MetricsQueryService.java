package com.basebackend.observability.service;

import com.basebackend.observability.dto.MetricsQueryRequest;

import java.util.List;
import java.util.Map;

/**
 * 指标查询服务接口
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
public interface MetricsQueryService {
    
    /**
     * 查询指标数据
     */
    Map<String, Object> queryMetrics(MetricsQueryRequest request);
    
    /**
     * 获取所有可用指标
     */
    List<String> getAvailableMetrics();
    
    /**
     * 获取系统概览
     */
    Map<String, Object> getSystemOverview();
}
