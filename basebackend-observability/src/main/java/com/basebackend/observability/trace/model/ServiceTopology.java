package com.basebackend.observability.trace.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 服务拓扑图模型
 */
@Data
@Builder
public class ServiceTopology {
    
    /**
     * 节点列表
     */
    private List<ServiceNode> nodes;
    
    /**
     * 边列表
     */
    private List<ServiceEdge> edges;
    
    /**
     * 统计信息
     */
    private Map<String, Object> statistics;
}
