package com.basebackend.scheduler.camunda.dto;

import java.util.Map;

/**
 * 流程变量 DTO
 */
public record ProcessVariableDTO(
        String name,
        Object value,
        String type,
        Map<String, Object> valueInfo,
        String processInstanceId,
        String executionId,
        String taskId
) {
}
