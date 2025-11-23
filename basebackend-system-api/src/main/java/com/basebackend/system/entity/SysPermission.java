package com.basebackend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /**
     * 权限名称
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 权限标识
     */
    @TableField("permission_key")
    private String permissionKey;

    /**
     * API路径
     */
    @TableField("api_path")
    private String apiPath;

    /**
     * HTTP方法
     */
    @TableField("http_method")
    private String httpMethod;

    /**
     * 权限类型：1-菜单权限，2-按钮权限，3-API权限
     */
    @TableField("permission_type")
    private Integer permissionType;

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
}
