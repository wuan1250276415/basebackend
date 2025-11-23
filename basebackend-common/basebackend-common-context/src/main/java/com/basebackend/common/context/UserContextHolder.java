package com.basebackend.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 用户上下文持有者
 * <p>
 * 基于 TransmittableThreadLocal 实现，支持跨线程传递用户上下文。
 * 在异步、线程池等场景下，确保用户上下文能够正确传递。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 设置用户上下文
 * UserContextHolder.set(userContext);
 *
 * // 获取用户上下文
 * UserContext user = UserContextHolder.get();
 * Long userId = UserContextHolder.getUserId();
 *
 * // 清除上下文（重要：请求结束时必须调用）
 * UserContextHolder.clear();
 * }</pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>必须在请求处理完成后调用 {@link #clear()} 方法，避免内存泄漏</li>
 *   <li>建议在 Filter 或 Interceptor 中统一管理上下文的设置和清除</li>
 *   <li>使用线程池时，需要配合 TTL 的 TtlExecutors 包装线程池</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see UserContext
 * @see ContextHolder
 */
public final class UserContextHolder {

    private UserContextHolder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * ThreadLocal 存储用户上下文
     * <p>
     * 使用 TransmittableThreadLocal 支持跨线程传递。
     * </p>
     */
    private static final TransmittableThreadLocal<UserContext> CONTEXT = new TransmittableThreadLocal<>();

    // ========== 核心方法 ==========

    /**
     * 设置用户上下文
     *
     * @param context 用户上下文对象
     */
    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取用户上下文（Optional 包装）
     *
     * @return 用户上下文 Optional
     */
    public static Optional<UserContext> getOptional() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * 获取用户上下文
     * <p>
     * 如果上下文不存在，返回 null。
     * 推荐使用 {@link #getOptional()} 避免 NPE。
     * </p>
     *
     * @return 用户上下文对象，可能为 null
     */
    public static UserContext get() {
        return CONTEXT.get();
    }

    /**
     * 获取用户上下文（带默认值）
     *
     * @param defaultValue 默认值
     * @return 用户上下文对象，如果不存在则返回默认值
     */
    public static UserContext getOrDefault(UserContext defaultValue) {
        UserContext context = CONTEXT.get();
        return context != null ? context : defaultValue;
    }

    /**
     * 清除用户上下文
     * <p>
     * 在请求处理完成后应调用此方法，避免内存泄漏。
     * </p>
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 判断用户上下文是否存在
     *
     * @return 是否存在上下文
     */
    public static boolean isPresent() {
        return CONTEXT.get() != null;
    }

    // ========== 便捷访问器方法 ==========

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果上下文不存在则返回 null
     */
    public static Long getUserId() {
        UserContext context = CONTEXT.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果上下文不存在则返回 null
     */
    public static String getUsername() {
        UserContext context = CONTEXT.get();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取当前用户昵称
     *
     * @return 用户昵称，如果上下文不存在则返回 null
     */
    public static String getNickname() {
        UserContext context = CONTEXT.get();
        return context != null ? context.getNickname() : null;
    }

    /**
     * 获取当前用户部门ID
     *
     * @return 部门ID，如果上下文不存在则返回 null
     */
    public static Long getDeptId() {
        UserContext context = CONTEXT.get();
        return context != null ? context.getDeptId() : null;
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param roleCode 角色编码
     * @return 是否拥有该角色，如果上下文不存在则返回 false
     */
    public static boolean hasRole(String roleCode) {
        UserContext context = CONTEXT.get();
        return context != null && context.hasRole(roleCode);
    }

    /**
     * 判断当前用户是否拥有指定权限
     *
     * @param permission 权限编码
     * @return 是否拥有该权限，如果上下文不存在则返回 false
     */
    public static boolean hasPermission(String permission) {
        UserContext context = CONTEXT.get();
        return context != null && context.hasPermission(permission);
    }

    /**
     * 判断当前用户是否为超级管理员
     *
     * @return 是否为超级管理员，如果上下文不存在则返回 false
     */
    public static boolean isSuperAdmin() {
        UserContext context = CONTEXT.get();
        return context != null && context.isAdmin();
    }


    /**
     * 要求用户上下文必须存在
     * <p>
     * 如果上下文不存在，抛出 IllegalStateException。
     * </p>
     *
     * @return 用户上下文对象
     * @throws IllegalStateException 如果上下文不存在
     */
    public static UserContext require() {
        UserContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("User context is not present");
        }
        return context;
    }

    /**
     * 要求用户ID必须存在
     * <p>
     * 如果上下文不存在或用户ID为 null，抛出 IllegalStateException。
     * </p>
     *
     * @return 用户ID
     * @throws IllegalStateException 如果上下文不存在或用户ID为 null
     */
    public static Long requireUserId() {
        UserContext context = require();
        Long userId = context.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID is not present in context");
        }
        return userId;
    }
}
