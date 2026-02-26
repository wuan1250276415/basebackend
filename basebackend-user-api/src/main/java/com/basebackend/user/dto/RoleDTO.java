package com.basebackend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 角色DTO
 */
public record RoleDTO(

    /**
     * 角色ID
     */
    Long id,

    /**
     * 所属应用ID
     */
    Long appId,

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 30, message = "角色名称长度不能超过30个字符")
    String roleName,

    /**
     * 角色标识
     */
    @NotBlank(message = "角色标识不能为空")
    @Size(max = 100, message = "角色标识长度不能超过100个字符")
    String roleKey,

    /**
     * 显示顺序
     */
    Integer roleSort,

    /**
     * 数据范围：1-全部数据权限，2-本部门数据权限，3-本部门及以下数据权限，4-仅本人数据权限
     */
    Integer dataScope,

    /**
     * 状态：0-禁用，1-启用
     */
    Integer status,

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    String remark,

    /**
     * 菜单ID列表
     */
    List<Long> menuIds,

    /**
     * 权限ID列表
     */
    List<Long> permissionIds
) {}
