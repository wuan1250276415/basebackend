package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程实例删除请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ProcessInstanceDeleteRequest", description = "流程实例删除请求参数")
public class ProcessInstanceDeleteRequest {

    /**
     * 删除原因
     */
    @Schema(
        description = "删除原因",
        example = "Cancelled by user request"
    )
    private String deleteReason = "deleted by API";

    /**
     * 是否跳过自定义监听器
     */
    @Schema(
        description = "是否跳过自定义监听器，默认 true",
        defaultValue = "true"
    )
    private boolean skipCustomListeners = true;

    /**
     * 是否跳过 IO 映射
     */
    @Schema(
        description = "是否跳过 IO 映射，默认 true",
        defaultValue = "true"
    )
    private boolean skipIoMappings = true;

    /**
     * 是否标记为外部终止
     */
    @Schema(
        description = "是否标记为外部终止，默认 true",
        defaultValue = "true"
    )
    private boolean externallyTerminated = true;

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 获取跳过自定义监听器标记（兼容性方法）
     * @return 是否跳过自定义监听器
     */
    public boolean getSkipCustomListeners() {
        return this.skipCustomListeners;
    }

    /**
     * 判断是否跳过自定义监听器（isXXX方法，用于主代码兼容）
     * @return 是否跳过自定义监听器
     */
    public boolean isSkipCustomListeners() {
        return this.skipCustomListeners;
    }

    /**
     * 获取跳过IO映射标记（兼容性方法）
     * @return 是否跳过IO映射
     */
    public boolean getSkipIoMappings() {
        return this.skipIoMappings;
    }

    /**
     * 判断是否跳过IO映射（isXXX方法，用于主代码兼容）
     * @return 是否跳过IO映射
     */
    public boolean isSkipIoMappings() {
        return this.skipIoMappings;
    }

    /**
     * 获取外部终止标记（兼容性方法）
     * @return 是否外部终止
     */
    public boolean getExternallyTerminated() {
        return this.externallyTerminated;
    }

    /**
     * 判断是否外部终止（isXXX方法，用于主代码兼容）
     * @return 是否外部终止
     */
    public boolean isExternallyTerminated() {
        return this.externallyTerminated;
    }
}
