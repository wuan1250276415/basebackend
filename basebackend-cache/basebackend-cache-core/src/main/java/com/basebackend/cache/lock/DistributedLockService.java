package com.basebackend.cache.lock;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务接口
 * 提供各种类型的分布式锁操作
 */
public interface DistributedLockService {

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间（租约时间）
     * @param unit 时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁
     *
     * @param lockKey 锁键
     */
    void unlock(String lockKey);

    /**
     * 执行带锁的操作
     *
     * @param lockKey 锁键
     * @param action 要执行的操作
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T executeWithLock(String lockKey, Supplier<T> action, long waitTime, long leaseTime);

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param lockKey 锁键
     * @param action 要执行的操作
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     */
    void executeWithLock(String lockKey, Runnable action, long waitTime, long leaseTime);

    /**
     * 获取公平锁
     * 按请求顺序分配锁
     *
     * @param lockKey 锁键
     * @return 公平锁实例
     */
    RLock getFairLock(String lockKey);

    /**
     * 获取联锁（MultiLock）
     * 同时获取多个锁或全部失败
     *
     * @param lockKeys 锁键数组
     * @return 联锁实例
     */
    RLock getMultiLock(String... lockKeys);

    /**
     * 获取红锁（RedLock）
     * 在多个 Redis 实例上获取锁以提高可靠性
     *
     * @param lockKey 锁键
     * @return 红锁实例
     */
    RLock getRedLock(String lockKey);

    /**
     * 获取读写锁
     *
     * @param lockKey 锁键
     * @return 读写锁实例
     */
    RReadWriteLock getReadWriteLock(String lockKey);

    /**
     * 尝试获取读锁
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @return 是否成功获取读锁
     */
    boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放读锁
     *
     * @param lockKey 锁键
     */
    void unlockRead(String lockKey);

    /**
     * 尝试获取写锁
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @return 是否成功获取写锁
     */
    boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放写锁
     *
     * @param lockKey 锁键
     */
    void unlockWrite(String lockKey);

    /**
     * 检查锁是否被当前线程持有
     *
     * @param lockKey 锁键
     * @return 是否被当前线程持有
     */
    boolean isHeldByCurrentThread(String lockKey);

    /**
     * 检查锁是否被锁定
     *
     * @param lockKey 锁键
     * @return 是否被锁定
     */
    boolean isLocked(String lockKey);
}
