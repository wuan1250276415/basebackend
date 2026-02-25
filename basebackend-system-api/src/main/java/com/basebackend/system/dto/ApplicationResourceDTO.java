package com.basebackend.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 应用资源DTO
 */
@Schema(description = "应用资源DTO")
public record ApplicationResourceDTO(
    @Schema(description = "资源ID")
    Long id,
    @Schema(description = "所属应用ID", required = true)
    @NotNull(message = "应用ID不能为空")
    Long appId,
    @Schema(description = "资源名称", required = true)
    @NotBlank(message = "资源名称不能为空")
    String resourceName,
    @Schema(description = "父资源ID")
    Long parentId,
    @Schema(description = "资源类型：M-目录，C-菜单，F-按钮", required = true)
    @NotBlank(message = "资源类型不能为空")
    String resourceType,
    @Schema(description = "路由地址")
    String path,
    @Schema(description = "组件路径")
    String component,
    @Schema(description = "权限标识")
    String perms,
    @Schema(description = "菜单图标")
    String icon,
    @Schema(description = "是否显示：0-隐藏，1-显示")
    Integer visible,
    @Schema(description = "打开方式：current-当前页，blank-新窗口")
    String openType,
    @Schema(description = "显示顺序")
    Integer orderNum,
    @Schema(description = "状态：0-禁用，1-启用")
    Integer status,
    @Schema(description = "备注")
    String remark,
    @Schema(description = "子资源列表")
    List<ApplicationResourceDTO> children,
    @Schema(description = "应用名称")
    String appName
) {}
