package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redisson的分布式锁管理器实现
 * <p>
 * 提供分布式环境下的锁服务，确保备份任务在多实例部署时不会并发执行。
 * 主要特性：
 * <ul>
 *   <li>支持可配置的等待时间和租约时间</li>
 *   <li>支持Runnable和Callable两种回调模式</li>
 *   <li>自动释放锁，防止死锁</li>
 *   <li>支持中断处理，保证线程安全</li>
 * </ul>
 *
 * @author BaseBackend
 * @see LockManager
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockManager implements LockManager {

    private final RedissonClient redissonClient;
    private final BackupProperties backupProperties;

    @Override
    public void withLock(String lockKey, Runnable action) throws Exception {
        withLock(lockKey, backupProperties.getDistributedLock().getWaitTime().toMillis(),
            backupProperties.getDistributedLock().getTtl().toMillis(), action);
    }

    @Override
    public void withLock(String lockKey, long waitTimeMs, long leaseTimeMs, Runnable action) throws Exception {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            log.debug("尝试获取分布式锁: {}, 等待时间: {}ms, 租约时间: {}ms",
                lockKey, waitTimeMs, leaseTimeMs);

            if (lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS)) {
                log.debug("成功获取分布式锁: {}", lockKey);
                try {
                    action.run();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("释放分布式锁: {}", lockKey);
                    }
                }
            } else {
                throw new IllegalStateException("获取分布式锁失败: " + lockKey + ", 等待超时: " + waitTimeMs + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", lockKey, e);
            throw new IllegalStateException("获取分布式锁被中断: " + lockKey, e);
        }
    }

    @Override
    public <T> T withLock(String lockKey, Callable<T> action) throws Exception {
        return withLock(lockKey, backupProperties.getDistributedLock().getWaitTime().toMillis(),
            backupProperties.getDistributedLock().getTtl().toMillis(), action);
    }

    @Override
    public <T> T withLock(String lockKey, long waitTimeMs, long leaseTimeMs, Callable<T> action) throws Exception {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            log.debug("尝试获取分布式锁: {}, 等待时间: {}ms, 租约时间: {}ms",
                lockKey, waitTimeMs, leaseTimeMs);

            if (lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS)) {
                log.debug("成功获取分布式锁: {}", lockKey);
                try {
                    return action.call();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("释放分布式锁: {}", lockKey);
                    }
                }
            } else {
                throw new IllegalStateException("获取分布式锁失败: " + lockKey + ", 等待超时: " + waitTimeMs + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", lockKey, e);
            throw new IllegalStateException("获取分布式锁被中断: " + lockKey, e);
        }
    }

    @Override
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, 0);
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, backupProperties.getDistributedLock().getTtl().toMillis(),
                TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("尝试获取分布式锁被中断: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("手动释放分布式锁: {}", lockKey);
            } else {
                log.warn("当前线程未持有锁，无法释放: {}", lockKey);
            }
        } catch (IllegalMonitorStateException e) {
            log.warn("锁状态异常，可能已被释放: {}", lockKey, e);
        }
    }

    @Override
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}
