package com.basebackend.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 菜单DTO
 */
@Data
public class MenuDTO {

    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 应用ID（为空表示系统菜单）
     */
    private Long appId;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 路由地址
     */
    @Size(max = 200, message = "路由地址长度不能超过200个字符")
    private String path;

    /**
     * 组件路径
     */
    @Size(max = 255, message = "组件路径长度不能超过255个字符")
    private String component;

    /**
     * 路由参数
     */
    @Size(max = 255, message = "路由参数长度不能超过255个字符")
    private String query;

    /**
     * 是否为外链：0-是，1-否
     */
    private Integer isFrame;

    /**
     * 是否缓存：0-缓存，1-不缓存
     */
    private Integer isCache;

    /**
     * 菜单类型：M-目录，C-菜单，F-按钮
     */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    /**
     * 菜单状态：0-隐藏，1-显示
     */
    private Integer visible;

    /**
     * 菜单状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 权限标识
     */
    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String perms;

    /**
     * 菜单图标
     */
    @Size(max = 100, message = "菜单图标长度不能超过100个字符")
    private String icon;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /**
     * 子菜单列表
     */
    private List<MenuDTO> children;
}
