package com.basebackend.api.model.scheduler;

import java.io.Serializable;
import java.util.Map;

/**
 * 流程定义启动请求 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record ProcessDefinitionStartRequest(
        String processDefinitionKey,
        String processDefinitionId,
        String businessKey,
        String tenantId,
        Map<String, Object> variables,
        String starter
) implements Serializable {
}
