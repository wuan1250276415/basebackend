package com.basebackend.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableField("parent_id")
    private Long parentId;

    @TableField("app_id")
    private Long appId;

    @TableField("role_name")
    private String roleName;

    @TableField("role_key")
    private String roleKey;

    @TableField("role_sort")
    private Integer roleSort;

    @TableField("data_scope")
    private Integer dataScope;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @TableField(exist = false)
    private List<SysRole> children;
}
