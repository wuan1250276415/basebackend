package com.basebackend.featuretoggle.context;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;

/**
 * 特性上下文持有器
 * <p>
 * 使用 ThreadLocal 存储当前线程的特性上下文信息，
 * 支持在非HTTP场景下传递上下文。
 * </p>
 *
 * @author BaseBackend
 */
public final class FeatureContextHolder {

    private static final ThreadLocal<FeatureContext> CONTEXT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Variant> VARIANT_HOLDER = new ThreadLocal<>();

    private FeatureContextHolder() {
        // 工具类，禁止实例化
    }

    /**
     * 设置当前线程的特性上下文
     *
     * @param context 特性上下文
     */
    public static void set(FeatureContext context) {
        if (context != null) {
            CONTEXT_HOLDER.set(context);
        } else {
            CONTEXT_HOLDER.remove();
        }
    }

    /**
     * 获取当前线程的特性上下文
     *
     * @return 特性上下文，如果未设置则返回空上下文
     */
    public static FeatureContext get() {
        FeatureContext context = CONTEXT_HOLDER.get();
        return context != null ? context : FeatureContext.empty();
    }

    /**
     * 获取当前线程的特性上下文（可能为null）
     *
     * @return 特性上下文或null
     */
    public static FeatureContext getOrNull() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除当前线程的特性上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
        VARIANT_HOLDER.remove();
    }

    /**
     * 设置当前变体（A/B测试使用）
     *
     * @param variant 变体信息
     */
    public static void setCurrentVariant(Variant variant) {
        if (variant != null) {
            VARIANT_HOLDER.set(variant);
        } else {
            VARIANT_HOLDER.remove();
        }
    }

    /**
     * 获取当前变体
     *
     * @return 变体信息或null
     */
    public static Variant getCurrentVariant() {
        return VARIANT_HOLDER.get();
    }

    /**
     * 清除当前变体
     */
    public static void clearCurrentVariant() {
        VARIANT_HOLDER.remove();
    }

    /**
     * 在指定上下文中执行操作
     *
     * @param context  特性上下文
     * @param runnable 要执行的操作
     */
    public static void runWithContext(FeatureContext context, Runnable runnable) {
        FeatureContext previous = CONTEXT_HOLDER.get();
        try {
            set(context);
            runnable.run();
        } finally {
            if (previous != null) {
                set(previous);
            } else {
                clear();
            }
        }
    }
}
