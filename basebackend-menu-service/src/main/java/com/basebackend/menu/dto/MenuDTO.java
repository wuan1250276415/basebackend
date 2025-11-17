package com.basebackend.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单DTO（Menu风格的API接口）
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Data
@Schema(description = "菜单DTO")
public class MenuDTO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "所属应用ID")
    private Long appId;

    @Schema(description = "菜单名称", required = true)
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "路由地址")
    @Size(max = 200, message = "路由地址长度不能超过200个字符")
    private String path;

    @Schema(description = "组件路径")
    @Size(max = 255, message = "组件路径长度不能超过255个字符")
    private String component;

    @Schema(description = "路由参数")
    private String query;

    @Schema(description = "是否为外链：0-是，1-否")
    private Integer isFrame;

    @Schema(description = "是否缓存：0-缓存，1-不缓存")
    private Integer isCache;

    @Schema(description = "菜单类型：M-目录，C-菜单，F-按钮", required = true)
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @Schema(description = "显示状态：0-隐藏，1-显示")
    private Integer visible;

    @Schema(description = "菜单状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "权限标识")
    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String perms;

    @Schema(description = "菜单图标")
    @Size(max = 100, message = "菜单图标长度不能超过100个字符")
    private String icon;

    @Schema(description = "打开方式：current-当前页，blank-新窗口")
    private String openType;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    @Schema(description = "子菜单列表")
    private List<MenuDTO> children;

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
