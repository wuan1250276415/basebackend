package com.basebackend.logging.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 热点日志缓存注解
 *
 * 用于标记需要进行热点日志缓存的方法，通过AOP拦截实现自动缓存管理。
 * 支持自定义缓存键、TTL、缓存策略和热点阈值。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HotLoggable {

    /**
     * 自定义缓存键
     * 如果为空，使用方法签名+参数哈希作为缓存键
     *
     * @return 缓存键
     */
    String cacheKey() default "";

    /**
     * TTL（秒）覆盖
     * -1表示使用全局配置
     *
     * @return TTL秒数
     */
    long ttlSeconds() default -1;

    /**
     * 缓存策略
     *
     * @return 缓存策略
     */
    CacheStrategy strategy() default CacheStrategy.READ_THROUGH;

    /**
     * 发生异常时是否失效缓存
     *
     * @return true=失效，false=保持
     */
    boolean invalidateOnException() default true;

    /**
     * 热点阈值覆盖
     * -1表示使用全局配置
     *
     * @return 访问次数阈值
     */
    int hotThreshold() default -1;

    /**
     * 是否预热缓存
     * 标记为true的方法会在启动时被预加载到缓存
     *
     * @return 是否预热
     */
    boolean preload() default false;

    /**
     * 缓存策略枚举
     */
    enum CacheStrategy {
        /**
         * 读透（Read-Through）
         * 查询时先查缓存，缓存未命中则执行方法并将结果放入缓存
         * 适用于：频繁查询的场景，如日志列表、统计信息等
         */
        READ_THROUGH,

        /**
         * 写透（Write-Through）
         * 写入成功后同步更新缓存
         * 适用于：需要强一致性的写入场景
         */
        WRITE_THROUGH,

        /**
         * 写回（Write-Back）
         * 写入成功后异步更新缓存（当前实现等同于写透）
         * 适用于：写入后需要立即查询的场景
         */
        WRITE_BACK,

        /**
         * 失效（Invalidate）
         * 写入成功后删除缓存条目
         * 适用于：写入后需要刷新数据的场景，如审计日志新增
         */
        INVALIDATE
    }
}
