package com.basebackend.featuretoggle.annotation;

import java.lang.annotation.*;

/**
 * 灰度发布注解
 * 用于渐进式发布新功能
 *
 * @author BaseBackend
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GradualRollout {

    /**
     * 特性名称
     */
    String value();

    /**
     * 期望的发布百分比（0-100）
     */
    int percentage() default 100;

    /**
     * 是否基于用户ID进行一致性分配
     */
    boolean stickySession() default true;

    /**
     * 未命中灰度时的降级策略
     */
    FallbackStrategy fallbackStrategy() default FallbackStrategy.RETURN_NULL;

    /**
     * 降级策略枚举
     */
    enum FallbackStrategy {
        /**
         * 返回null
         */
        RETURN_NULL,

        /**
         * 抛出异常
         */
        THROW_EXCEPTION,

        /**
         * 执行降级方法
         */
        FALLBACK_METHOD
    }

    /**
     * 降级方法名称（当fallbackStrategy=FALLBACK_METHOD时使用）
     */
    String fallbackMethod() default "";
}
