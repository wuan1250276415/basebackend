package com.basebackend.feign.dto.scheduler;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务操作请求 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record TaskActionRequest(
        /** 任务ID */
        String taskId,

        /** 操作用户 */
        String userId,

        /** 操作变量 */
        Map<String, Object> variables,

        /** 本地变量 */
        Map<String, Object> localVariables,

        /** 任务结果或意见 */
        String comment
) implements Serializable {
}
