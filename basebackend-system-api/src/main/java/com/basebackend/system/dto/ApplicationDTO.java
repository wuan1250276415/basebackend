package com.basebackend.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 应用信息DTO
 */
@Schema(description = "应用信息DTO")
public record ApplicationDTO(
        @Schema(description = "应用ID")
        Long id,

        @Schema(description = "应用名称", required = true)
        @NotBlank(message = "应用名称不能为空")
        String appName,

        @Schema(description = "应用编码", required = true)
        @NotBlank(message = "应用编码不能为空")
        String appCode,

        @Schema(description = "应用类型", required = true)
        @NotBlank(message = "应用类型不能为空")
        String appType,

        @Schema(description = "应用图标")
        String appIcon,

        @Schema(description = "应用地址")
        String appUrl,

        @Schema(description = "是否启用：0-禁用，1-启用", required = true)
        @NotNull(message = "启用状态不能为空")
        Integer status,

        @Schema(description = "显示顺序")
        Integer orderNum,

        @Schema(description = "备注")
        String remark
) {
}
