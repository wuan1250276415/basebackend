package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 认领任务请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ClaimTaskRequest", description = "认领任务请求")
public class ClaimTaskRequest {

    /**
     * 用户 ID
     */
    @Schema(
        description = "用户 ID",
        example = "alice",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;
}
