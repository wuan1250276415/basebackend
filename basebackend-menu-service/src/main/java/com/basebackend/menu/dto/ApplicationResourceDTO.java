package com.basebackend.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用资源DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Data
@Schema(description = "应用资源DTO")
public class ApplicationResourceDTO {

    @Schema(description = "资源ID")
    private Long id;

    @Schema(description = "所属应用ID", required = true)
    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @Schema(description = "资源名称", required = true)
    @NotBlank(message = "资源名称不能为空")
    private String resourceName;

    @Schema(description = "父资源ID")
    private Long parentId;

    @Schema(description = "资源类型：M-目录，C-菜单，F-按钮", required = true)
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "是否显示：0-隐藏，1-显示")
    private Integer visible;

    @Schema(description = "打开方式：current-当前页，blank-新窗口")
    private String openType;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子资源列表")
    private List<ApplicationResourceDTO> children;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "更新人")
    private Long updateBy;
}
