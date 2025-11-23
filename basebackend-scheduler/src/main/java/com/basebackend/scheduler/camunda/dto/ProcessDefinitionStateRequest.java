package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * 流程定义状态变更请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ProcessDefinitionStateRequest", description = "流程定义状态变更请求")
public class ProcessDefinitionStateRequest {

    /**
     * 是否包含已存在的流程实例
     */
    @Schema(
        description = "是否同时挂起/激活已存在的流程实例，默认 true",
        defaultValue = "true",
        example = "true"
    )
    private boolean includeProcessInstances = true;

    /**
     * 执行时间（ISO-8601 格式），null 表示立即执行
     */
    @Schema(
        description = "执行时间（ISO-8601 格式，如 2025-01-01T10:00:00Z），null 表示立即执行",
        example = "2025-01-01T10:00:00Z"
    )
    private Instant executeAt;

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 获取包含流程实例标记（兼容性方法）
     * @return 是否包含流程实例
     */
    public boolean getIncludeProcessInstances() {
        return this.includeProcessInstances;
    }

    /**
     * 判断是否包含流程实例（isXXX方法，用于主代码兼容）
     * @return 是否包含流程实例
     */
    public boolean isIncludeProcessInstances() {
        return this.includeProcessInstances;
    }
}
