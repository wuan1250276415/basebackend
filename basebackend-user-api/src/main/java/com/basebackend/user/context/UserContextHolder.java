package com.basebackend.user.context;

import com.basebackend.user.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 用户上下文持有者
 * 使用 ThreadLocal 存储当前请求的用户上下文
 * <p>
 * 类似于前端的 useUser() Hook，提供便捷的用户信息访问方法
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-09
 */
@Slf4j
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户上下文
     *
     * @param userContext 用户上下文
     */
    public static void setContext(UserContext userContext) {
        CONTEXT_HOLDER.set(userContext);
        log.debug("设置用户上下文: userId={}, username={}",
                userContext.getUserId(), userContext.getUsername());
    }

    /**
     * 获取用户上下文
     *
     * @return 用户上下文，如果不存在则返回 null
     */
    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取用户上下文 Optional
     *
     * @return Optional<UserContext>
     */
    public static Optional<UserContext> getContextOptional() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    /**
     * 清空用户上下文
     * 重要：请求结束后必须调用此方法，防止内存泄漏
     */
    public static void clear() {
        UserContext context = CONTEXT_HOLDER.get();
        if (context != null) {
            log.debug("清空用户上下文: userId={}, username={}",
                    context.getUserId(), context.getUsername());
        }
        CONTEXT_HOLDER.remove();
    }

    // ==================== 便捷方法：快速获取用户信息 ====================

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未登录则返回 null
     */
    public static Long getUserId() {
        return getContextOptional()
                .map(UserContext::getUserId)
                .orElse(null);
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未登录则返回 null
     */
    public static String getUsername() {
        return getContextOptional()
                .map(UserContext::getUsername)
                .orElse(null);
    }

    /**
     * 获取当前用户昵称
     *
     * @return 昵称，如果未登录则返回 null
     */
    public static String getNickname() {
        return getContextOptional()
                .map(UserContext::getNickname)
                .orElse(null);
    }

    /**
     * 获取当前用户部门ID
     *
     * @return 部门ID，如果未登录则返回 null
     */
    public static Long getDeptId() {
        return getContextOptional()
                .map(UserContext::getDeptId)
                .orElse(null);
    }

    /**
     * 获取当前用户部门名称
     *
     * @return 部门名称，如果未登录则返回 null
     */
    public static String getDeptName() {
        return getContextOptional()
                .map(UserContext::getDeptName)
                .orElse(null);
    }

    /**
     * 获取当前用户IP地址
     *
     * @return IP地址，如果未登录则返回 null
     */
    public static String getIpAddress() {
        return getContextOptional()
                .map(UserContext::getIpAddress)
                .orElse(null);
    }

    // ==================== 便捷方法：权限和角色检查 ====================

    /**
     * 检查当前用户是否有指定权限
     *
     * @param permission 权限标识（如：system:user:add）
     * @return true-有权限，false-无权限或未登录
     */
    public static boolean hasPermission(String permission) {
        return getContextOptional()
                .map(context -> context.hasPermission(permission))
                .orElse(false);
    }

    /**
     * 检查当前用户是否有任意一个权限
     *
     * @param permissions 权限列表
     * @return true-有权限，false-无权限或未登录
     */
    public static boolean hasAnyPermission(String... permissions) {
        return getContextOptional()
                .map(context -> context.hasAnyPermission(permissions))
                .orElse(false);
    }

    /**
     * 检查当前用户是否有所有权限
     *
     * @param permissions 权限列表
     * @return true-有权限，false-无权限或未登录
     */
    public static boolean hasAllPermissions(String... permissions) {
        return getContextOptional()
                .map(context -> context.hasAllPermissions(permissions))
                .orElse(false);
    }

    /**
     * 检查当前用户是否有指定角色
     *
     * @param role 角色Key（如：admin）
     * @return true-有角色，false-无角色或未登录
     */
    public static boolean hasRole(String role) {
        return getContextOptional()
                .map(context -> context.hasRole(role))
                .orElse(false);
    }

    /**
     * 检查当前用户是否有任意一个角色
     *
     * @param roles 角色列表
     * @return true-有角色，false-无角色或未登录
     */
    public static boolean hasAnyRole(String... roles) {
        return getContextOptional()
                .map(context -> context.hasAnyRole(roles))
                .orElse(false);
    }

    /**
     * 检查当前用户是否是管理员
     *
     * @return true-是管理员，false-不是管理员或未登录
     */
    public static boolean isAdmin() {
        return getContextOptional()
                .map(UserContext::isAdmin)
                .orElse(false);
    }

    /**
     * 检查当前用户是否是系统用户
     *
     * @return true-是系统用户，false-不是系统用户或未登录
     */
    public static boolean isSystemUser() {
        return getContextOptional()
                .map(UserContext::isSystemUser)
                .orElse(false);
    }

    /**
     * 检查当前用户是否启用
     *
     * @return true-启用，false-禁用或未登录
     */
    public static boolean isEnabled() {
        return getContextOptional()
                .map(UserContext::isEnabled)
                .orElse(false);
    }

    /**
     * 检查当前是否已登录
     *
     * @return true-已登录，false-未登录
     */
    public static boolean isAuthenticated() {
        return CONTEXT_HOLDER.get() != null;
    }

    /**
     * 要求当前必须已登录，否则抛出异常
     *
     * @return 用户上下文
     * @throws IllegalStateException 如果未登录
     */
    public static UserContext requireAuthenticated() {
        UserContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            throw new IllegalStateException("用户未登录");
        }
        return context;
    }

    /**
     * 要求当前用户有指定权限，否则抛出异常
     *
     * @param permission 权限标识
     * @throws IllegalStateException 如果无权限
     */
    public static void requirePermission(String permission) {
        if (!hasPermission(permission)) {
            throw new IllegalStateException("用户没有权限: " + permission);
        }
    }

    /**
     * 要求当前用户有指定角色，否则抛出异常
     *
     * @param role 角色Key
     * @throws IllegalStateException 如果无角色
     */
    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new IllegalStateException("用户没有角色: " + role);
        }
    }
}
