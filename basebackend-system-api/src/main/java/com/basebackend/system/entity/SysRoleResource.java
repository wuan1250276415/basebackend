package com.basebackend.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色资源关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_resource")
public class SysRoleResource extends BaseEntity {

    @TableField("role_id")
    private Long roleId;

    @TableField("resource_id")
    private Long resourceId;
}
