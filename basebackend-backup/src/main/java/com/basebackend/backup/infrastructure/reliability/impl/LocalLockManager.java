package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.infrastructure.reliability.LockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地锁管理器（Redisson 不可用时的 fallback）
 * <p>
 * 使用 JVM 本地的 ReentrantLock 实现，适用于单实例部署。
 * 当 Redisson 可用时，此 Bean 不会被注册。
 */
@Slf4j
@Component
@ConditionalOnMissingBean(LockManager.class)
public class LocalLockManager implements LockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(String lockKey) {
        return locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
    }

    @Override
    public void withLock(String lockKey, Runnable action) throws Exception {
        withLock(lockKey, 30000, 60000, action);
    }

    @Override
    public <T> T withLock(String lockKey, Callable<T> action) throws Exception {
        return withLock(lockKey, 30000, 60000, action);
    }

    @Override
    public void withLock(String lockKey, long waitTimeMs, long leaseTimeMs, Runnable action) throws Exception {
        ReentrantLock lock = getLock(lockKey);
        if (lock.tryLock(waitTimeMs, TimeUnit.MILLISECONDS)) {
            try {
                action.run();
            } finally {
                lock.unlock();
            }
        } else {
            throw new IllegalStateException("获取本地锁失败: " + lockKey);
        }
    }

    @Override
    public <T> T withLock(String lockKey, long waitTimeMs, long leaseTimeMs, Callable<T> action) throws Exception {
        ReentrantLock lock = getLock(lockKey);
        if (lock.tryLock(waitTimeMs, TimeUnit.MILLISECONDS)) {
            try {
                return action.call();
            } finally {
                lock.unlock();
            }
        } else {
            throw new IllegalStateException("获取本地锁失败: " + lockKey);
        }
    }

    @Override
    public boolean tryLock(String lockKey) {
        return getLock(lockKey).tryLock();
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime) {
        try {
            return getLock(lockKey).tryLock(waitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        ReentrantLock lock = locks.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isHeldByCurrentThread(String lockKey) {
        ReentrantLock lock = locks.get(lockKey);
        return lock != null && lock.isHeldByCurrentThread();
    }

    @Override
    public boolean isLocked(String lockKey) {
        ReentrantLock lock = locks.get(lockKey);
        return lock != null && lock.isLocked();
    }
}
