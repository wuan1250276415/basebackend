package com.basebackend.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

/**
 * 租户上下文持有者
 * <p>
 * 基于 TransmittableThreadLocal 实现，支持跨线程传递租户上下文。
 * 在多租户 SaaS 架构中，确保租户上下文能够正确传递到各个业务层。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 设置租户上下文
 * TenantContextHolder.set(tenantContext);
 *
 * // 获取租户上下文
 * TenantContextInfo tenant = TenantContextHolder.get();
 * Long tenantId = TenantContextHolder.getTenantId();
 *
 * // 清除上下文（重要：请求结束时必须调用）
 * TenantContextHolder.clear();
 * }</pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>必须在请求处理完成后调用 {@link #clear()} 方法，避免内存泄漏</li>
 *   <li>建议在 Filter 或 Interceptor 中统一管理上下文的设置和清除</li>
 *   <li>租户上下文通常在用户上下文之前设置，用于数据隔离</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see TenantContextInfo
 * @see ContextHolder
 */
public final class TenantContextHolder {

    private TenantContextHolder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * ThreadLocal 存储租户上下文
     * <p>
     * 使用 TransmittableThreadLocal 支持跨线程传递。
     * </p>
     */
    private static final TransmittableThreadLocal<TenantContextInfo> CONTEXT = new TransmittableThreadLocal<>();

    // ========== 核心方法 ==========

    /**
     * 设置租户上下文
     *
     * @param context 租户上下文对象
     */
    public static void set(TenantContextInfo context) {
        CONTEXT.set(context);
    }

    /**
     * 获取租户上下文（Optional 包装）
     *
     * @return 租户上下文 Optional
     */
    public static Optional<TenantContextInfo> getOptional() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * 获取租户上下文
     * <p>
     * 如果上下文不存在，返回 null。
     * 推荐使用 {@link #getOptional()} 避免 NPE。
     * </p>
     *
     * @return 租户上下文对象，可能为 null
     */
    public static TenantContextInfo get() {
        return CONTEXT.get();
    }

    /**
     * 获取租户上下文（带默认值）
     *
     * @param defaultValue 默认值
     * @return 租户上下文对象，如果不存在则返回默认值
     */
    public static TenantContextInfo getOrDefault(TenantContextInfo defaultValue) {
        TenantContextInfo context = CONTEXT.get();
        return context != null ? context : defaultValue;
    }

    /**
     * 清除租户上下文
     * <p>
     * 在请求处理完成后应调用此方法，避免内存泄漏。
     * </p>
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 判断租户上下文是否存在
     *
     * @return 是否存在上下文
     */
    public static boolean isPresent() {
        return CONTEXT.get() != null;
    }

    // ========== 便捷访问器方法 ==========

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，如果上下文不存在则返回 null
     */
    public static Long getTenantId() {
        TenantContextInfo context = CONTEXT.get();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * 获取当前租户编码
     *
     * @return 租户编码，如果上下文不存在则返回 null
     */
    public static String getTenantCode() {
        TenantContextInfo context = CONTEXT.get();
        return context != null ? context.getTenantCode() : null;
    }

    /**
     * 获取当前租户名称
     *
     * @return 租户名称，如果上下文不存在则返回 null
     */
    public static String getTenantName() {
        TenantContextInfo context = CONTEXT.get();
        return context != null ? context.getTenantName() : null;
    }

    /**
     * 判断当前租户是否正常启用
     *
     * @return 是否启用，如果上下文不存在则返回 false
     */
    public static boolean isEnabled() {
        TenantContextInfo context = CONTEXT.get();
        return context != null && context.isEnabled();
    }

    /**
     * 判断当前租户是否被禁用
     *
     * @return 是否禁用，如果上下文不存在则返回 false
     */
    public static boolean isDisabled() {
        TenantContextInfo context = CONTEXT.get();
        return context != null && context.isDisabled();
    }

    /**
     * 判断当前租户是否已过期
     *
     * @return 是否过期，如果上下文不存在则返回 false
     */
    public static boolean isExpired() {
        TenantContextInfo context = CONTEXT.get();
        return context != null && context.isExpired();
    }

    /**
     * 获取当前租户套餐编码
     *
     * @return 套餐编码，如果上下文不存在则返回 null
     */
    public static String getPackageCode() {
        TenantContextInfo context = CONTEXT.get();
        return context != null ? context.getPackageCode() : null;
    }

    /**
     * 获取当前租户数据隔离模式
     *
     * @return 隔离模式，如果上下文不存在则返回 null
     */
    public static String getIsolationMode() {
        TenantContextInfo context = CONTEXT.get();
        return context != null ? context.getIsolationMode() : null;
    }

    /**
     * 判断是否为列级隔离模式
     *
     * @return 是否为列级隔离，如果上下文不存在则返回 false
     */
    public static boolean isColumnIsolation() {
        String mode = getIsolationMode();
        return "COLUMN".equalsIgnoreCase(mode);
    }

    /**
     * 判断是否为 Schema 级隔离模式
     *
     * @return 是否为 Schema 级隔离，如果上下文不存在则返回 false
     */
    public static boolean isSchemaIsolation() {
        String mode = getIsolationMode();
        return "SCHEMA".equalsIgnoreCase(mode);
    }

    /**
     * 判断是否为数据库级隔离模式
     *
     * @return 是否为数据库级隔离，如果上下文不存在则返回 false
     */
    public static boolean isDatabaseIsolation() {
        String mode = getIsolationMode();
        return "DATABASE".equalsIgnoreCase(mode);
    }

    /**
     * 要求租户上下文必须存在
     * <p>
     * 如果上下文不存在，抛出 IllegalStateException。
     * </p>
     *
     * @return 租户上下文对象
     * @throws IllegalStateException 如果上下文不存在
     */
    public static TenantContextInfo require() {
        TenantContextInfo context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("Tenant context is not present");
        }
        return context;
    }

    /**
     * 要求租户ID必须存在
     * <p>
     * 如果上下文不存在或租户ID为 null，抛出 IllegalStateException。
     * </p>
     *
     * @return 租户ID
     * @throws IllegalStateException 如果上下文不存在或租户ID为 null
     */
    public static Long requireTenantId() {
        TenantContextInfo context = require();
        Long tenantId = context.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not present in context");
        }
        return tenantId;
    }

    /**
     * 忽略租户上下文执行操作
     * <p>
     * 临时清除租户上下文，执行操作后恢复。
     * 适用于需要访问全局数据（跨租户数据）的场景。
     * </p>
     *
     * @param action 要执行的操作
     */
    public static void ignoreTenant(Runnable action) {
        TenantContextInfo backup = CONTEXT.get();
        try {
            CONTEXT.remove();
            action.run();
        } finally {
            if (backup != null) {
                CONTEXT.set(backup);
            }
        }
    }

    /**
     * 忽略租户上下文执行操作（带返回值）
     * <p>
     * 临时清除租户上下文，执行操作后恢复。
     * 适用于需要访问全局数据（跨租户数据）的场景。
     * </p>
     *
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作返回值
     */
    public static <T> T ignoreTenant(java.util.function.Supplier<T> supplier) {
        TenantContextInfo backup = CONTEXT.get();
        try {
            CONTEXT.remove();
            return supplier.get();
        } finally {
            if (backup != null) {
                CONTEXT.set(backup);
            }
        }
    }
}
