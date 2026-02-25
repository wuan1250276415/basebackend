package com.basebackend.cache.util;

import com.basebackend.cache.exception.CacheLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁工具类
 * 增强版，提供更多锁类型和便捷方法
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
     * 执行带锁的操作（有返回值）
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        boolean locked = tryLock(lockKey, waitTime, leaseTime, unit);
        if (!locked) {
            throw new CacheLockException("Failed to acquire lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @param action 要执行的操作
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
        executeWithLock(lockKey, waitTime, leaseTime, unit, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 获取公平锁
     * 按请求顺序分配锁
     */
    public RLock getFairLock(String lockKey) {
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 尝试获取公平锁
     */
    public boolean tryFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getFairLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("获取公平锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 获取联锁（MultiLock）
     * 同时获取多个锁或全部失败
     */
    public RLock getMultiLock(String... lockKeys) {
        if (lockKeys == null || lockKeys.length == 0) {
            throw new IllegalArgumentException("Lock keys cannot be null or empty");
        }

        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = redissonClient.getLock(lockKeys[i]);
        }

        return redissonClient.getMultiLock(locks);
    }

    /**
     * 尝试获取联锁
     */
    public boolean tryMultiLock(long waitTime, long leaseTime, TimeUnit unit, String... lockKeys) {
        RLock multiLock = getMultiLock(lockKeys);
        try {
            return multiLock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("获取联锁失败: {}", String.join(",", lockKeys), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放联锁
     */
    public void unlockMultiLock(String... lockKeys) {
        RLock multiLock = getMultiLock(lockKeys);
        if (multiLock.isHeldByCurrentThread()) {
            multiLock.unlock();
        }
    }

    /**
     * 获取红锁（RedLock）
     * 注意：需要配置多个 Redis 实例才能真正实现红锁
     * 当前实现使用单个实例作为后备方案
     */
    public RLock getRedLock(String lockKey) {
        // 在生产环境中，应该配置多个 Redis 实例
        // 例如: redissonClient.getRedLock(lock1, lock2, lock3);
        log.debug("Creating red lock (using regular lock as fallback): {}", lockKey);
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取读写锁
     */
    public RReadWriteLock getReadWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey);
    }

    /**
     * 尝试获取读锁
     */
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        try {
            return readLock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("获取读锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放读锁
     */
    public void unlockRead(String lockKey) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        if (readLock.isHeldByCurrentThread()) {
            readLock.unlock();
        }
    }

    /**
     * 尝试获取写锁
     */
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        try {
            return writeLock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("获取写锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放写锁
     */
    public void unlockWrite(String lockKey) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        if (writeLock.isHeldByCurrentThread()) {
            writeLock.unlock();
        }
    }

    /**
     * 执行带读锁的操作
     */
    public <T> T executeWithReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        boolean locked = tryReadLock(lockKey, waitTime, leaseTime, unit);
        if (!locked) {
            throw new CacheLockException("Failed to acquire read lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            unlockRead(lockKey);
        }
    }

    /**
     * 执行带写锁的操作
     */
    public <T> T executeWithWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        boolean locked = tryWriteLock(lockKey, waitTime, leaseTime, unit);
        if (!locked) {
            throw new CacheLockException("Failed to acquire write lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            unlockWrite(lockKey);
        }
    }

    /**
     * 检查锁是否被当前线程持有
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 检查锁是否被锁定
     */
    public boolean isLocked(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isLocked();
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
