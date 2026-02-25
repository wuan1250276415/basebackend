package com.basebackend.feign.dto.scheduler;

import java.io.Serializable;
import java.util.Map;

/**
 * 启动流程实例请求 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record ProcessDefinitionStartRequest(
        /** 流程定义键 */
        String processDefinitionKey,

        /** 流程定义ID（优先级高于key） */
        String processDefinitionId,

        /** 业务键 */
        String businessKey,

        /** 租户ID */
        String tenantId,

        /** 流程变量 */
        Map<String, Object> variables,

        /** 启动人 */
        String starter
) implements Serializable {
}
