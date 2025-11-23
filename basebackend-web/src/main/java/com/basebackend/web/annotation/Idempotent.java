package com.basebackend.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性保证注解
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等性key的前缀，默认为类名.方法名
     */
    String keyPrefix() default "";

    /**
     * 过期时间（秒），默认300秒
     */
    long expireTime() default 300L;

    /**
     * 业务标识（用于区分不同业务场景）
     */
    String bizId() default "";

    /**
     * 重复请求的处理策略
     */
    Strategy strategy() default Strategy.REJECT;

    /**
     * 自定义提示信息
     */
    String message() default "请求已处理，请勿重复提交";

    /**
     * 处理策略
     */
    enum Strategy {
        /**
         * 拒绝重复请求
         */
        REJECT,
        /**
         * 返回第一个请求的结果
         */
        RETURN_RESULT,
        /**
         * 等待处理完成（同步等待）
         */
        WAIT
    }
}
