package com.basebackend.file.security;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户认证上下文
 *
 * 存储当前请求的用户身份信息和客户端信息，用于权限检查和审计
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Data
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（必填）
     */
    private String userId;

    /**
     * 租户ID（可选）
     */
    private String tenantId;

    /**
     * 用户角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 客户端 IP 地址
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 认证方式
     */
    private AuthType authType;

    /**
     * Token 标识符（JWT 的 jti 或内部追踪ID）
     */
    private String tokenId;

    /**
     * 认证时间
     */
    private LocalDateTime authenticatedAt;

    /**
     * 认证类型枚举
     */
    public enum AuthType {
        /**
         * JWT Token 认证
         */
        JWT,

        /**
         * API Key 认证
         */
        API_KEY,

        /**
         * 网关传递的用户ID（信任的内网调用）
         */
        GATEWAY_TRUSTED
    }

    /**
     * 是否具有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 是否具有任一指定角色
     */
    public boolean hasAnyRole(String... roles) {
        if (this.roles == null || roles == null) {
            return false;
        }
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否具有所有指定角色
     */
    public boolean hasAllRoles(String... roles) {
        if (this.roles == null || roles == null) {
            return false;
        }
        for (String role : roles) {
            if (!this.roles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否具有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 是否具有任一指定权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (this.permissions == null || permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为管理员（简化版）
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SYSTEM_ADMIN");
    }

    /**
     * 检查是否为资源所有者（基于 userId）
     */
    public boolean isOwner(String resourceOwnerId) {
        return userId != null && userId.equals(resourceOwnerId);
    }
}
