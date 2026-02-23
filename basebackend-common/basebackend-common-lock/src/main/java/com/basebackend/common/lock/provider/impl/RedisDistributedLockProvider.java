package com.basebackend.common.lock.provider.impl;

import com.basebackend.common.lock.provider.DistributedLockProvider;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式锁实现
 * <p>
 * 支持可重入锁和自动续期（Redisson watchdog 机制）。
 * 当 leaseTime 设为 -1 时启用 watchdog 自动续期。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class RedisDistributedLockProvider implements DistributedLockProvider {

    private final RedissonClient redissonClient;

    public RedisDistributedLockProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断, key={}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String key) {
        return redissonClient.getLock(key).isLocked();
    }

    @Override
    public void forceUnlock(String key) {
        redissonClient.getLock(key).forceUnlock();
    }
}
