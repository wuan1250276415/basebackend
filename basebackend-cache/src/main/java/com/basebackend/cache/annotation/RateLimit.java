package com.basebackend.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RateType;

/**
 * 方法级限流注解
 * 基于 Redisson RRateLimiter 实现分布式限流
 *
 * 使用示例：
 * <pre>
 * {@code
 * @RateLimit(key = "'api:user:' + #userId", rate = 10, interval = 60)
 * public Result queryUser(Long userId) {
 *     return userService.findById(userId);
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键，支持 SpEL 表达式
     * 为空时默认使用 className:methodName
     */
    String key() default "";

    /**
     * 每个时间窗口内允许的请求数
     */
    long rate() default 100;

    /**
     * 时间窗口大小
     */
    long interval() default 60;

    /**
     * 时间窗口单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流模式
     * OVERALL: 全局共享限流
     * PER_CLIENT: 按客户端独立限流
     */
    RateType mode() default RateType.OVERALL;
}
