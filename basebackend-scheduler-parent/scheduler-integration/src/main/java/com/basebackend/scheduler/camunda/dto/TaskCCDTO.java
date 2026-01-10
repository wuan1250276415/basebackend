package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务抄送 DTO
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@Schema(description = "任务抄送信息")
public class TaskCCDTO {

    @Schema(description = "抄送记录ID")
    private Long id;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程定义Key")
    private String processDefinitionKey;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "抄送发起人ID")
    private String initiatorId;

    @Schema(description = "状态: UNREAD, READ")
    private String status;

    @Schema(description = "抄送时间")
    private LocalDateTime createTime;
}
