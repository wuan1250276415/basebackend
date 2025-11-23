package com.basebackend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.annotation.SensitiveType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

import static com.basebackend.database.security.context.PermissionContext.VIEW_PHONE;

/**
 * 系统部门实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /**
     * 部门名称
     */
    @TableField("dept_name")
    private String deptName;

    /**
     * 父部门ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 显示顺序
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 负责人
     */
    @TableField("leader")
    private String leader;

    /**
     * 联系电话
     */
    @TableField("phone")
    @Sensitive(type = SensitiveType.PHONE,requiredPermission = VIEW_PHONE)
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 部门状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 子部门列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysDept> children;
}
