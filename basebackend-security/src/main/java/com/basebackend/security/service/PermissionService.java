package com.basebackend.security.service;

import java.util.List;

/**
 * 权限服务接口
 * 
 * 各个微服务需要实现此接口来提供权限验证功能
 */
public interface PermissionService {

    /**
     * 获取当前用户的权限列表
     * 
     * @return 权限标识列表
     */
    List<String> getCurrentUserPermissions();

    /**
     * 获取当前用户的角色列表
     * 
     * @return 角色标识列表
     */
    List<String> getCurrentUserRoles();

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    Long getCurrentUserId();

    /**
     * 获取当前用户的部门ID
     * 
     * @return 部门ID
     */
    Long getCurrentUserDeptId();

    /**
     * 检查用户是否有指定权限
     *
     * @param permission 权限标识
     * @return 是否有权限
     */
    default boolean hasPermission(String permission) {
        List<String> permissions = getCurrentUserPermissions();
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 检查用户是否有指定角色
     *
     * @param role 角色标识
     * @return 是否有角色
     */
    default boolean hasRole(String role) {
        List<String> roles = getCurrentUserRoles();
        return roles != null && roles.contains(role);
    }
}
