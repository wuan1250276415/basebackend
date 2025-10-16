package com.basebackend.cache.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockUtil {

    private final RedissonClient redissonClient;

    /**
     * 获取可重入锁
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁key
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("获取锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 获取读写锁
     */
    public RReadWriteLock getReadWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey);
    }

    /**
     * 获取信号量
     */
    public RSemaphore getSemaphore(String semaphoreKey) {
        return redissonClient.getSemaphore(semaphoreKey);
    }

    /**
     * 获取倒计时锁
     */
    public RCountDownLatch getCountDownLatch(String latchKey) {
        return redissonClient.getCountDownLatch(latchKey);
    }
}
