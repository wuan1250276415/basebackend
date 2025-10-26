package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件权限实体
 */
@Data
@TableName("file_permission")
public class FilePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 权限类型(READ/WRITE/DELETE/SHARE)
     */
    private String permissionType;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 授权人ID
     */
    private Long grantedBy;

    /**
     * 授权人名称
     */
    private String grantedByName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
