package com.basebackend.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sentinel 限流注解
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流资源名称，默认为方法名称
     */
    String resource() default "";

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.QPS;

    /**
     * 限流阈值
     */
    double threshold() default 100.0;

    /**
     * 限流策略（DIRECT/WARM_UP/MATE_RATE_LIMITER）
     */
    String strategy() default "DIRECT";

    /**
     * 限流控制行为（FAST_FAIL/WARM_UP/MATE_RATE_LIMITER）
     */
    String controlBehavior() default "FAST_FAIL";

    /**
     * 熔断阈值（异常比例 0.0-1.0）
     */
    double degradeRatio() default 0.5;

    /**
     * 熔断最小请求数
     */
    int degradeMinRequestAmount() default 5;

    /**
     * 熔断统计时长（毫秒）
     */
    int degradeStatIntervalMs() default 1000;

    /**
     * 熔断恢复时长（毫秒）
     */
    long degradeRecoveryTimeMs() default 3000;

    /**
     * 提示信息
     */
    String message() default "系统繁忙，请稍后重试";

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * QPS 模式
         */
        QPS,
        /**
         * 并发线程数模式
         */
        THREAD
    }
}
