package com.basebackend.common.lock;

import com.basebackend.common.lock.aspect.DistributedLockAspect;
import com.basebackend.common.lock.config.LockProperties;
import com.basebackend.common.lock.exception.LockAcquisitionException;
import com.basebackend.common.lock.provider.DistributedLockProvider;
import com.basebackend.common.lock.provider.impl.InMemoryDistributedLockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式锁单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
class DistributedLockTest {

    private InMemoryDistributedLockProvider lockProvider;

    @BeforeEach
    void setUp() {
        lockProvider = new InMemoryDistributedLockProvider();
    }

    @Test
    @DisplayName("内存锁 - 获取和释放")
    void testAcquireAndRelease() {
        String key = "test:lock:1";
        boolean acquired = lockProvider.tryLock(key, 1, 30, TimeUnit.SECONDS);
        assertTrue(acquired, "应当成功获取锁");
        assertTrue(lockProvider.isLocked(key), "锁应当处于锁定状态");

        lockProvider.unlock(key);
        assertFalse(lockProvider.isLocked(key), "释放后锁应当不再锁定");
    }

    @Test
    @DisplayName("内存锁 - 可重入")
    void testReentrant() {
        String key = "test:lock:reentrant";

        assertTrue(lockProvider.tryLock(key, 1, 30, TimeUnit.SECONDS));
        // 同一线程再次获取应当成功（可重入）
        assertTrue(lockProvider.tryLock(key, 1, 30, TimeUnit.SECONDS));

        lockProvider.unlock(key);
        // 释放一次后仍应处于锁定状态（重入计数未归零）
        assertTrue(lockProvider.isLocked(key));

        lockProvider.unlock(key);
        assertFalse(lockProvider.isLocked(key));
    }

    @Test
    @DisplayName("内存锁 - 超时获取失败")
    void testTimeoutAcquisition() throws InterruptedException {
        String key = "test:lock:timeout";
        AtomicBoolean otherThreadAcquired = new AtomicBoolean(false);
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch testDone = new CountDownLatch(1);

        // 在另一个线程中先持有锁
        Thread holder = new Thread(() -> {
            lockProvider.tryLock(key, 1, 30, TimeUnit.SECONDS);
            lockHeld.countDown();
            try {
                testDone.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lockProvider.unlock(key);
            }
        });
        holder.start();

        lockHeld.await(5, TimeUnit.SECONDS);

        // 当前线程尝试获取应当超时失败
        boolean acquired = lockProvider.tryLock(key, 100, 30, TimeUnit.MILLISECONDS);
        assertFalse(acquired, "锁被其他线程持有时应当获取失败");

        testDone.countDown();
        holder.join(5000);
    }

    @Test
    @DisplayName("内存锁 - 强制释放")
    void testForceUnlock() {
        String key = "test:lock:force";
        lockProvider.tryLock(key, 1, 30, TimeUnit.SECONDS);
        assertTrue(lockProvider.isLocked(key));

        lockProvider.forceUnlock(key);
        assertFalse(lockProvider.isLocked(key));
    }

    @Test
    @DisplayName("LockAcquisitionException 构造")
    void testLockAcquisitionException() {
        LockAcquisitionException ex = new LockAcquisitionException("操作频繁");
        assertEquals("操作频繁", ex.getMessage());
        assertEquals(429, ex.getCode());
    }

    @Test
    @DisplayName("LockProperties 默认值")
    void testLockPropertiesDefaults() {
        LockProperties props = new LockProperties();
        assertEquals("redis", props.getType());
        assertEquals("", props.getKeyPrefix());
    }
}
