package com.basebackend.admin.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 用户上下文
 * 存储当前请求的用户信息、权限、角色等
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 用户类型：1-系统用户，2-普通用户
     */
    private Integer userType;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 角色Key列表（如：admin, user）
     */
    private Set<String> roles;

    /**
     * 权限列表（如：system:user:add）
     */
    private Set<String> permissions;

    /**
     * 请求IP地址
     */
    private String ipAddress;

    /**
     * 请求时间戳
     */
    private Long requestTime;

    /**
     * 检查是否有指定权限
     *
     * @param permission 权限标识
     * @return true-有权限，false-无权限
     */
    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        // 支持通配符 * (如：system:user:*)
        if (permissions.contains("*:*:*") || permissions.contains("*")) {
            return true;
        }
        return permissions.contains(permission);
    }

    /**
     * 检查是否有任意一个权限
     *
     * @param permissions 权限列表
     * @return true-有权限，false-无权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否有所有权限
     *
     * @param permissions 权限列表
     * @return true-有权限，false-无权限
     */
    public boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否有指定角色
     *
     * @param role 角色Key
     * @return true-有角色，false-无角色
     */
    public boolean hasRole(String role) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        // 支持超级管理员
        if (roles.contains("admin") || roles.contains("super_admin")) {
            return true;
        }
        return roles.contains(role);
    }

    /**
     * 检查是否有任意一个角色
     *
     * @param roles 角色列表
     * @return true-有角色，false-无角色
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否是管理员
     *
     * @return true-是管理员，false-不是管理员
     */
    public boolean isAdmin() {
        return hasRole("admin") || hasRole("super_admin");
    }

    /**
     * 检查是否是系统用户
     *
     * @return true-是系统用户，false-不是系统用户
     */
    public boolean isSystemUser() {
        return userType != null && userType == 1;
    }

    /**
     * 检查用户是否启用
     *
     * @return true-启用，false-禁用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
