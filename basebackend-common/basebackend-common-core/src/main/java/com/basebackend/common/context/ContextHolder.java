package com.basebackend.common.context;

import java.util.Optional;

/**
 * 上下文持有者接口
 * <p>
 * 定义上下文管理的标准契约，提供上下文的设置、获取和清除操作。
 * 具体实现通常基于 ThreadLocal 或 TransmittableThreadLocal。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>提供统一的上下文管理 API</li>
 *   <li>支持泛型，可用于用户上下文、租户上下文等</li>
 *   <li>支持 Optional 返回，避免 NPE</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 实现用户上下文持有者
 * public class UserContextHolder implements ContextHolder<UserContextInfo> {
 *     private static final ThreadLocal<UserContextInfo> CONTEXT = new ThreadLocal<>();
 *
 *     @Override
 *     public void set(UserContextInfo context) { CONTEXT.set(context); }
 *
 *     @Override
 *     public Optional<UserContextInfo> getOptional() { return Optional.ofNullable(CONTEXT.get()); }
 *
 *     @Override
 *     public void clear() { CONTEXT.remove(); }
 * }
 * }</pre>
 *
 * @param <T> 上下文类型
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface ContextHolder<T> {

    /**
     * 设置上下文
     *
     * @param context 上下文对象
     */
    void set(T context);

    /**
     * 获取上下文（Optional 包装）
     *
     * @return 上下文 Optional
     */
    Optional<T> getOptional();

    /**
     * 获取上下文
     * <p>
     * 如果上下文不存在，返回 null。
     * 推荐使用 {@link #getOptional()} 避免 NPE。
     * </p>
     *
     * @return 上下文对象，可能为 null
     */
    default T get() {
        return getOptional().orElse(null);
    }

    /**
     * 获取上下文（带默认值）
     *
     * @param defaultValue 默认值
     * @return 上下文对象，如果不存在则返回默认值
     */
    default T getOrDefault(T defaultValue) {
        return getOptional().orElse(defaultValue);
    }

    /**
     * 判断上下文是否存在
     *
     * @return 是否存在上下文
     */
    default boolean isPresent() {
        return getOptional().isPresent();
    }

    /**
     * 清除上下文
     * <p>
     * 在请求处理完成后应调用此方法，避免内存泄漏。
     * </p>
     */
    void clear();
}
