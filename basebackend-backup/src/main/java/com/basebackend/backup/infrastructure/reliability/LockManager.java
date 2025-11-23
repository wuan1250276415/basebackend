package com.basebackend.backup.infrastructure.reliability;

import java.util.concurrent.Callable;

/**
 * 锁管理器接口
 * 抽象分布式锁实现，支持可重入锁和超时控制
 */
public interface LockManager {

    /**
     * 在锁保护下执行操作
     *
     * @param lockKey 锁键
     * @param action 要执行的操作
     * @throws Exception 执行失败时抛出异常
     */
    void withLock(String lockKey, Runnable action) throws Exception;

    /**
     * 在锁保护下执行操作（支持返回值）
     *
     * @param <T> 返回值类型
     * @param lockKey 锁键
     * @param action 要执行的操作
     * @return 操作执行结果
     * @throws Exception 执行失败时抛出异常
     */
    <T> T withLock(String lockKey, Callable<T> action) throws Exception;

    /**
     * 在锁保护下执行操作（带超时）
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 租约时间
     * @param action 要执行的操作
     * @throws Exception 执行失败时抛出异常
     */
    void withLock(String lockKey, long waitTime, long leaseTime, Runnable action) throws Exception;

    /**
     * 在锁保护下执行操作（带超时，支持返回值）
     *
     * @param <T> 返回值类型
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 租约时间
     * @param action 要执行的操作
     * @return 操作执行结果
     * @throws Exception 执行失败时抛出异常
     */
    <T> T withLock(String lockKey, long waitTime, long leaseTime, Callable<T> action) throws Exception;

    /**
     * 尝试获取锁（不阻塞）
     *
     * @param lockKey 锁键
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey);

    /**
     * 尝试获取锁（带超时）
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间（毫秒）
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, long waitTime);

    /**
     * 释放锁
     *
     * @param lockKey 锁键
     */
    void unlock(String lockKey);

    /**
     * 检查锁是否被当前线程持有
     *
     * @param lockKey 锁键
     * @return 是否持有锁
     */
    boolean isHeldByCurrentThread(String lockKey);

    /**
     * 获取锁的持有状态
     *
     * @param lockKey 锁键
     * @return 是否被任何线程持有
     */
    boolean isLocked(String lockKey);
}
