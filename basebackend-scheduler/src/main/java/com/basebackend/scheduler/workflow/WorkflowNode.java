package com.basebackend.scheduler.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 工作流节点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID（唯一标识）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型: task/condition/parallel/end
     */
    @Builder.Default
    private String nodeType = "task";

    /**
     * 处理器类名
     */
    private String processorType;

    /**
     * 节点参数
     */
    private Map<String, Object> params;

    /**
     * 条件表达式（仅nodeType=condition时使用）
     * 例如: result.status == 'SUCCESS'
     */
    private String condition;

    /**
     * 是否允许失败后继续
     */
    @Builder.Default
    private Boolean allowFailure = false;

    /**
     * 超时时间(秒)
     */
    @Builder.Default
    private Integer timeout = 300;
}
