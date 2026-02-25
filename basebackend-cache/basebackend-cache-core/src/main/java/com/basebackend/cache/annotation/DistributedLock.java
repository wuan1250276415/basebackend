package com.basebackend.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 用于方法级别的分布式锁控制
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的键
     * 支持 SpEL 表达式，例如: "user:#{#userId}"
     */
    String key();

    /**
     * 等待获取锁的时间
     * 默认 3 秒
     */
    long waitTime() default 3;

    /**
     * 锁的持有时间（租约时间）
     * 默认 10 秒
     * -1 表示不自动释放（需要手动释放或等待看门狗机制）
     */
    long leaseTime() default 10;

    /**
     * 时间单位
     * 默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 锁类型
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 获取锁失败时是否抛出异常
     * true: 抛出 CacheLockException
     * false: 返回 null 或默认值
     */
    boolean throwException() default true;

    /**
     * 锁类型枚举
     */
    enum LockType {
        /**
         * 可重入锁（默认）
         */
        REENTRANT,

        /**
         * 公平锁
         */
        FAIR,

        /**
         * 读锁
         */
        READ,

        /**
         * 写锁
         */
        WRITE
    }
}
