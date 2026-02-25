package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 任务抄送 DTO
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Schema(description = "任务抄送信息")
public record TaskCCDTO(
        @Schema(description = "抄送记录ID")
        Long id,

        @Schema(description = "任务ID")
        String taskId,

        @Schema(description = "流程实例ID")
        String processInstanceId,

        @Schema(description = "流程定义Key")
        String processDefinitionKey,

        @Schema(description = "任务名称")
        String taskName,

        @Schema(description = "抄送发起人ID")
        String initiatorId,

        @Schema(description = "状态: UNREAD, READ")
        String status,

        @Schema(description = "抄送时间")
        LocalDateTime createTime
) {
}
