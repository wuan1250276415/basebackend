package com.basebackend.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /**
     * 应用ID（为空表示系统菜单）
     */
    @TableField("app_id")
    private Long appId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String menuName;

    /**
     * 父菜单ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 路由地址
     */
    @TableField("path")
    private String path;

    /**
     * 组件路径
     */
    @TableField("component")
    private String component;

    /**
     * 路由参数
     */
    @TableField("query")
    private String query;

    /**
     * 是否为外链：0-是，1-否
     */
    @TableField("is_frame")
    private Integer isFrame;

    /**
     * 是否缓存：0-缓存，1-不缓存
     */
    @TableField("is_cache")
    private Integer isCache;

    /**
     * 菜单类型：M-目录，C-菜单，F-按钮
     */
    @TableField("menu_type")
    private String menuType;

    /**
     * 菜单状态：0-隐藏，1-显示
     */
    @TableField("visible")
    private Integer visible;

    /**
     * 菜单状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 权限标识
     */
    @TableField("perms")
    private String perms;

    /**
     * 菜单图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 子菜单列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysMenu> children;
}
