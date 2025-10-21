package com.basebackend.scheduler.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工作流边（任务依赖关系）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源节点ID
     */
    private String fromNodeId;

    /**
     * 目标节点ID
     */
    private String toNodeId;

    /**
     * 边的条件（可选）
     * 仅当源节点满足此条件时，才执行目标节点
     */
    private String condition;
}
