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
     * 源流程定义 ID
     */
    @Schema(
        description = "源流程定义 ID",
        example = "order_approval:2:12345",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "源流程定义 ID 不能为空")
    private String sourceProcessDefinitionId;

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
     * 待迁移的流程实例 ID 列表
     */
    @Schema(
        description = "待迁移的流程实例 ID 列表",
        example = "[\"instance-001\", \"instance-002\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<String> processInstanceIds = Collections.emptyList();

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

    // ========== Primitive getters for boolean fields ==========

    public boolean isSkipCustomListeners() {
        return Boolean.TRUE.equals(skipCustomListeners);
    }

    public boolean isSkipIoMappings() {
        return Boolean.TRUE.equals(skipIoMappings);
    }

    /**
     * 迁移指令 DTO
     */
    @Data
    @Schema(name = "MigrationInstructionDTO", description = "活动迁移映射指令")
    public static class MigrationInstructionDTO {

        @Schema(description = "源活动 ID", example = "UserTask_A")
        private String sourceActivityId;

        @Schema(description = "目标活动 ID", example = "UserTask_A_v2")
        private String targetActivityId;

        @Schema(description = "是否更新事件触发器", defaultValue = "false")
        private boolean updateEventTrigger = false;
    }
}
