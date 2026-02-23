package com.basebackend.common.lock.provider;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁提供者 SPI 接口
 * <p>
 * 定义分布式锁的核心操作契约，支持不同的底层实现（Redis、内存等）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface DistributedLockProvider {

    /**
     * 尝试获取锁
     *
     * @param key       锁的 key
     * @param waitTime  等待获取锁的最大时间
     * @param leaseTime 持有锁的最大时间
     * @param unit      时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁
     *
     * @param key 锁的 key
     */
    void unlock(String key);

    /**
     * 查询锁是否被持有
     *
     * @param key 锁的 key
     * @return 是否被锁定
     */
    boolean isLocked(String key);

    /**
     * 强制释放锁（不检查持有者）
     *
     * @param key 锁的 key
     */
    void forceUnlock(String key);
}
