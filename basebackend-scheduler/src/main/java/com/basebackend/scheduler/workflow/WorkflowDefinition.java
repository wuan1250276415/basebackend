package com.basebackend.scheduler.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 * 支持DAG有向无环图任务编排
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private Long id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 工作流节点列表
     */
    @Builder.Default
    private List<WorkflowNode> nodes = new ArrayList<>();

    /**
     * 工作流边列表（任务依赖关系）
     */
    @Builder.Default
    private List<WorkflowEdge> edges = new ArrayList<>();

    /**
     * 工作流参数
     */
    private Map<String, Object> params;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 添加节点
     */
    public void addNode(WorkflowNode node) {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(node);
    }

    /**
     * 添加边（依赖关系）
     */
    public void addEdge(WorkflowEdge edge) {
        if (edges == null) {
            edges = new ArrayList<>();
        }
        edges.add(edge);
    }

    /**
     * 添加边（简化方法）
     */
    public void addEdge(String fromNodeId, String toNodeId) {
        addEdge(WorkflowEdge.builder()
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .build());
    }
}
