package com.basebackend.file.security;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文持有者
 *
 * 使用 ThreadLocal 存储当前请求的用户上下文，确保线程安全
 * 在 Filter 的 finally 块中必须调用 clear() 清理，防止内存泄漏
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置用户上下文
     *
     * @param context 用户上下文
     */
    public static void setContext(UserContext context) {
        CONTEXT.set(context);
        log.trace("设置用户上下文: userId={}, tenantId={}",
                context != null ? context.getUserId() : "null",
                context != null ? context.getTenantId() : "null");
    }

    /**
     * 获取用户上下文
     *
     * @return 用户上下文，可能为 null
     */
    public static UserContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 获取用户上下文，如果为 null 则抛出异常
     *
     * @return 用户上下文
     * @throws IllegalStateException 如果上下文未设置
     */
    public static UserContext getContextOrThrow() {
        UserContext context = getContext();
        if (context == null) {
            throw new IllegalStateException("用户上下文未设置，请确保请求已通过认证过滤器");
        }
        return context;
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，可能为 null
     */
    public static String getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户ID，如果为 null 则抛出异常
     *
     * @return 用户ID
     * @throws IllegalStateException 如果用户未认证
     */
    public static String getCurrentUserIdOrThrow() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未认证，请检查认证过滤器配置");
        }
        return userId;
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，可能为 null
     */
    public static String getCurrentTenantId() {
        UserContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * 获取当前认证类型
     *
     * @return 认证类型，可能为 null
     */
    public static UserContext.AuthType getCurrentAuthType() {
        UserContext context = getContext();
        return context != null ? context.getAuthType() : null;
    }

    /**
     * 清理用户上下文
     *
     * <strong>重要：必须在 Filter 的 finally 块中调用此方法，防止内存泄漏！</strong>
     */
    public static void clear() {
        UserContext context = CONTEXT.get();
        if (context != null) {
            log.trace("清理用户上下文: userId={}", context.getUserId());
        } else {
            log.trace("清理用户上下文: 无上下文");
        }
        CONTEXT.remove();
    }

    /**
     * 检查当前用户是否为管理员
     *
     * @return 如果是管理员返回 true，否则返回 false
     */
    public static boolean isCurrentUserAdmin() {
        UserContext context = getContext();
        return context != null && context.isAdmin();
    }

    /**
     * 检查当前用户是否为资源所有者
     *
     * @param resourceOwnerId 资源所有者ID
     * @return 如果是所有者返回 true，否则返回 false
     */
    public static boolean isCurrentUserOwner(String resourceOwnerId) {
        UserContext context = getContext();
        return context != null && context.isOwner(resourceOwnerId);
    }

    /**
     * 检查当前用户是否有指定角色
     *
     * @param role 角色名称
     * @return 如果有角色返回 true，否则返回 false
     */
    public static boolean hasRole(String role) {
        UserContext context = getContext();
        return context != null && context.hasRole(role);
    }

    /**
     * 检查当前用户是否有指定权限
     *
     * @param permission 权限名称
     * @return 如果有权限返回 true，否则返回 false
     */
    public static boolean hasPermission(String permission) {
        UserContext context = getContext();
        return context != null && context.hasPermission(permission);
    }
}
