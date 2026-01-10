package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程实例终止请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "TerminateRequest", description = "流程实例终止请求参数")
public class TerminateRequest {

    /**
     * 终止原因
     */
    @Schema(
        description = "终止原因",
        example = "Cancelled by user request"
    )
    private String reason;
}
