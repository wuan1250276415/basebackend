package com.basebackend.common.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * <p>
 * 标注在方法上，自动获取/释放分布式锁。
 * 支持 SpEL 表达式动态生成锁 key。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @DistributedLock(key = "#userId", prefix = "user:lock:")
 * public void updateUser(Long userId) { ... }
 *
 * @DistributedLock(key = "#order.id", waitTime = 5, leaseTime = 60)
 * public void processOrder(Order order) { ... }
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的 key，支持 SpEL 表达式（如 #userId, #order.id）
     */
    String key();

    /**
     * key 前缀
     */
    String prefix() default "lock:";

    /**
     * 等待获取锁的最大时间
     */
    long waitTime() default 3;

    /**
     * 持有锁的最大时间
     */
    long leaseTime() default 30;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 获取失败时的错误消息
     */
    String errorMessage() default "操作频繁，请稍后再试";
}
