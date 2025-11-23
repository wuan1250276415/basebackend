package com.basebackend.cache.lock;

import com.basebackend.cache.exception.CacheLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Successfully acquired lock: {}", lockKey);
            } else {
                log.debug("Failed to acquire lock: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to acquire lock: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new CacheLockException("Failed to acquire lock: " + lockKey, e);
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Successfully released lock: {}", lockKey);
        } else {
            log.warn("Attempted to unlock a lock not held by current thread: {}", lockKey);
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action, long waitTime, long leaseTime) {
        boolean locked = tryLock(lockKey, waitTime, leaseTime, TimeUnit.SECONDS);
        if (!locked) {
            throw new CacheLockException("Failed to acquire lock within wait time: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            unlock(lockKey);
        }
    }

    @Override
    public void executeWithLock(String lockKey, Runnable action, long waitTime, long leaseTime) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        }, waitTime, leaseTime);
    }

    @Override
    public RLock getFairLock(String lockKey) {
        log.debug("Creating fair lock: {}", lockKey);
        return redissonClient.getFairLock(lockKey);
    }

    @Override
    public RLock getMultiLock(String... lockKeys) {
        if (lockKeys == null || lockKeys.length == 0) {
            throw new IllegalArgumentException("Lock keys cannot be null or empty");
        }

        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = redissonClient.getLock(lockKeys[i]);
        }

        log.debug("Creating multi-lock with {} locks", lockKeys.length);
        return redissonClient.getMultiLock(locks);
    }

    @Override
    public RLock getRedLock(String lockKey) {
        // RedLock requires multiple Redis instances
        // For now, we'll use a regular lock as a fallback
        // In production, configure multiple Redis instances and use:
        // return redissonClient.getRedLock(locks);
        log.debug("Creating red lock (using regular lock as fallback): {}", lockKey);
        return redissonClient.getLock(lockKey);
    }

    @Override
    public RReadWriteLock getReadWriteLock(String lockKey) {
        log.debug("Creating read-write lock: {}", lockKey);
        return redissonClient.getReadWriteLock(lockKey);
    }

    @Override
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        try {
            boolean acquired = readLock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Successfully acquired read lock: {}", lockKey);
            } else {
                log.debug("Failed to acquire read lock: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to acquire read lock: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new CacheLockException("Failed to acquire read lock: " + lockKey, e);
        }
    }

    @Override
    public void unlockRead(String lockKey) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        if (readLock.isHeldByCurrentThread()) {
            readLock.unlock();
            log.debug("Successfully released read lock: {}", lockKey);
        } else {
            log.warn("Attempted to unlock a read lock not held by current thread: {}", lockKey);
        }
    }

    @Override
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        try {
            boolean acquired = writeLock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Successfully acquired write lock: {}", lockKey);
            } else {
                log.debug("Failed to acquire write lock: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to acquire write lock: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new CacheLockException("Failed to acquire write lock: " + lockKey, e);
        }
    }

    @Override
    public void unlockWrite(String lockKey) {
        RReadWriteLock rwLock = getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        if (writeLock.isHeldByCurrentThread()) {
            writeLock.unlock();
            log.debug("Successfully released write lock: {}", lockKey);
        } else {
            log.warn("Attempted to unlock a write lock not held by current thread: {}", lockKey);
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
