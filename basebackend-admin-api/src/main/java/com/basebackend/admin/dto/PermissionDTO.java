package com.basebackend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 权限DTO
 */
public record PermissionDTO(
    /** 权限ID */
    Long id,
    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称长度不能超过50个字符")
    String permissionName,
    /** 权限标识 */
    @NotBlank(message = "权限标识不能为空")
    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    String permissionKey,
    /** API路径 */
    @Size(max = 200, message = "API路径长度不能超过200个字符")
    String apiPath,
    /** HTTP方法 */
    @Size(max = 10, message = "HTTP方法长度不能超过10个字符")
    String httpMethod,
    /** 权限类型：1-菜单权限，2-按钮权限，3-API权限 */
    Integer permissionType,
    /** 状态：0-禁用，1-启用 */
    Integer status,
    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    String remark
) {}
