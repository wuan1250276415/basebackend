package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 流程实例迁移请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ProcessInstanceMigrationRequest", description = "流程实例迁移与升级请求")
public class ProcessInstanceMigrationRequest {

    /**
     * 目标流程定义 ID
     */
    @Schema(
        description = "目标流程定义 ID",
        example = "order_approval:3:98765",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "目标流程定义 ID 不能为空")
    private String targetProcessDefinitionId;

    /**
     * 是否自动映射同名活动
     */
    @Schema(
        description = "是否自动映射同名活动，默认 true",
        defaultValue = "true"
    )
    private Boolean mapEqualActivities = Boolean.TRUE;

    /**
     * 是否跳过自定义监听器
     */
    @Schema(
        description = "是否跳过自定义监听器，默认 true",
        defaultValue = "true"
    )
    private Boolean skipCustomListeners = Boolean.TRUE;

    /**
     * 是否跳过 IO 映射
     */
    @Schema(
        description = "是否跳过 IO 映射，默认 true",
        defaultValue = "true"
    )
    private Boolean skipIoMappings = Boolean.TRUE;

    /**
     * 自定义迁移动作集合
     */
    @Schema(
        description = "自定义迁移动作集合，用于映射源活动到目标活动",
        example = "[{\"sourceActivityId\":\"UserTask_A\", \"targetActivityId\":\"UserTask_A_v2\"}]"
    )
    private List<MigrationInstructionDTO> instructions = Collections.emptyList();
}
