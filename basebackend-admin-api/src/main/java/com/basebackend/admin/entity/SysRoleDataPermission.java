package com.basebackend.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色数据权限配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_data_permission")
public class SysRoleDataPermission extends BaseEntity {

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 资源类型（如：user, dept, order等）
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 权限名称
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 过滤类型：dept-部门，field-字段，custom-自定义
     */
    @TableField("filter_type")
    private String filterType;

    /**
     * 过滤规则（JSON格式）
     * 示例：
     * 部门过滤：{"deptIds": [1, 2, 3]}
     * 字段过滤：{"fields": ["id", "name", "email"]}
     * 自定义过滤：{"conditions": [{"field": "createBy", "operator": "eq", "value": "{{currentUserId}}"}]}
     */
    @TableField("filter_rule")
    private String filterRule;

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
