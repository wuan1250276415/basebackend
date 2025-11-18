package com.basebackend.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 应用资源（菜单）实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_application_resource")
public class SysApplicationResource extends BaseEntity {

    /**
     * 所属应用ID
     */
    @TableField("app_id")
    private Long appId;

    /**
     * 资源名称
     */
    @TableField("resource_name")
    private String resourceName;

    /**
     * 父资源ID（0表示顶级）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 资源类型：M-目录，C-菜单，F-按钮
     */
    @TableField("resource_type")
    private String resourceType;

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
     * 是否显示：0-隐藏，1-显示
     */
    @TableField("visible")
    private Integer visible;

    /**
     * 打开方式：current-当前页，blank-新窗口
     */
    @TableField("open_type")
    private String openType;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 子资源列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysApplicationResource> children;

    /**
     * 应用名称（非数据库字段）
     */
    @TableField(exist = false)
    private String appName;
}
