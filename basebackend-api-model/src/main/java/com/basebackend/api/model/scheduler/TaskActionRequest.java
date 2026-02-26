package com.basebackend.api.model.scheduler;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务操作请求 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record TaskActionRequest(
        String taskId,
        String userId,
        Map<String, Object> variables,
        Map<String, Object> localVariables,
        String comment
) implements Serializable {
}
