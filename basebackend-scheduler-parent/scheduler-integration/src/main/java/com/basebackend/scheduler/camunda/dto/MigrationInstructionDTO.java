package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程实例迁移动作映射
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "MigrationInstructionDTO", description = "流程实例迁移动作映射")
public class MigrationInstructionDTO {

    /**
     * 源活动 ID
     */
    @Schema(
        description = "源活动 ID",
        example = "UserTask_Start",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "源活动 ID 不能为空")
    private String sourceActivityId;

    /**
     * 目标活动 ID
     */
    @Schema(
        description = "目标活动 ID",
        example = "UserTask_Start_v2",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "目标活动 ID 不能为空")
    private String targetActivityId;
}
