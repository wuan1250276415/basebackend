package com.basebackend.common.lock.provider.impl;

import com.basebackend.common.lock.provider.DistributedLockProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于内存的分布式锁实现（单机降级方案）
 * <p>
 * 使用 ConcurrentHashMap + ReentrantLock 实现，仅适用于单机环境。
 * 当 Redis 不可用时作为降级方案使用。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class InMemoryDistributedLockProvider implements DistributedLockProvider {

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            return lock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取内存锁被中断, key={}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        ReentrantLock lock = lockMap.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            // 无等待线程时清理，避免内存泄漏
            if (!lock.hasQueuedThreads() && !lock.isLocked()) {
                lockMap.remove(key, lock);
            }
        }
    }

    @Override
    public boolean isLocked(String key) {
        ReentrantLock lock = lockMap.get(key);
        return lock != null && lock.isLocked();
    }

    @Override
    public void forceUnlock(String key) {
        ReentrantLock lock = lockMap.get(key);
        if (lock == null) {
            return;
        }

        if (lock.isHeldByCurrentThread()) {
            // 当前线程持有锁时，释放所有重入层级
            while (lock.getHoldCount() > 0) {
                lock.unlock();
            }
            // 仅在确认无持有且无等待线程时清理映射，避免并发语义被破坏
            if (!lock.hasQueuedThreads() && !lock.isLocked()) {
                lockMap.remove(key, lock);
            }
            return;
        }

        // 非持有线程不允许移除仍在使用中的锁，只清理已空闲锁对象
        if (!lock.isLocked()) {
            lockMap.remove(key, lock);
        }
    }
}
