package com.basebackend.feign.dto.scheduler;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 启动流程实例请求 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class ProcessDefinitionStartRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义键
     */
    private String processDefinitionKey;

    /**
     * 流程定义ID（优先级高于key）
     */
    private String processDefinitionId;

    /**
     * 业务键
     */
    private String businessKey;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 启动人
     */
    private String starter;
}
