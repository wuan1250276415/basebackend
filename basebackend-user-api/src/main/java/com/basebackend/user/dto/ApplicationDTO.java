package com.basebackend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用信息DTO
 */
@Data
@Schema(description = "应用信息DTO")
public class ApplicationDTO {

    @Schema(description = "应用ID")
    private Long id;

    @Schema(description = "应用名称", required = true)
    @NotBlank(message = "应用名称不能为空")
    private String appName;

    @Schema(description = "应用编码", required = true)
    @NotBlank(message = "应用编码不能为空")
    private String appCode;

    @Schema(description = "应用类型", required = true)
    @NotBlank(message = "应用类型不能为空")
    private String appType;

    @Schema(description = "应用图标")
    private String appIcon;

    @Schema(description = "应用地址")
    private String appUrl;

    @Schema(description = "是否启用：0-禁用，1-启用", required = true)
    @NotNull(message = "启用状态不能为空")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "备注")
    private String remark;
}
