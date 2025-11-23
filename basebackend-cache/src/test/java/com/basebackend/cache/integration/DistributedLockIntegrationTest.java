package com.basebackend.cache.integration;

import com.basebackend.cache.config.CacheAutoConfiguration;
import com.basebackend.cache.lock.DistributedLockService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式锁集成测试
 * 测试并发场景下的分布式锁行为
 */
@SpringBootTest(classes = {CacheAutoConfiguration.class})
@Testcontainers
class DistributedLockIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("basebackend.cache.enabled", () -> "true");
    }

    @Autowired
    private DistributedLockService lockService;

    @Test
    void testBasicLock() {
        String lockKey = "test:lock:basic";

        // 获取锁
        boolean locked = lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);
        assertTrue(locked);
        assertTrue(lockService.isHeldByCurrentThread(lockKey));

        // 释放锁
        lockService.unlock(lockKey);
        assertFalse(lockService.isHeldByCurrentThread(lockKey));
    }

    @Test
    void testLockMutualExclusion() throws InterruptedException {
        String lockKey = "test:lock:mutex";
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 创建多个线程竞争同一个锁
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    if (lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS)) {
                        try {
                            // 模拟临界区操作
                            int current = counter.get();
                            Thread.sleep(10);
                            counter.set(current + 1);
                        } finally {
                            lockService.unlock(lockKey);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(30, TimeUnit.SECONDS);

        // 验证计数器值正确（证明互斥性）
        assertEquals(threadCount, counter.get());
    }

    @Test
    void testExecuteWithLock() {
        String lockKey = "test:lock:execute";

        // 使用 executeWithLock 执行操作
        String result = lockService.executeWithLock(lockKey, () -> {
            return "executed";
        }, 5, 10);

        assertEquals("executed", result);
    }

    @Test
    void testLockTimeout() throws InterruptedException {
        String lockKey = "test:lock:timeout";

        // 第一个线程获取锁
        boolean locked1 = lockService.tryLock(lockKey, 0, 2, TimeUnit.SECONDS);
        assertTrue(locked1);

        // 第二个线程尝试获取锁（应该失败，因为等待时间为0）
        boolean locked2 = lockService.tryLock(lockKey, 0, 2, TimeUnit.SECONDS);
        assertFalse(locked2);

        // 等待锁自动释放
        Thread.sleep(3000);

        // 现在应该可以获取锁了
        boolean locked3 = lockService.tryLock(lockKey, 0, 2, TimeUnit.SECONDS);
        assertTrue(locked3);
        lockService.unlock(lockKey);
    }

    @Test
    void testFairLock() throws InterruptedException {
        String lockKey = "test:lock:fair";
        RLock fairLock = lockService.getFairLock(lockKey);

        // 测试公平锁的基本功能
        boolean locked = fairLock.tryLock(5, 10, TimeUnit.SECONDS);
        assertTrue(locked);

        fairLock.unlock();
        assertFalse(fairLock.isHeldByCurrentThread());
    }

    @Test
    void testMultiLock() {
        String lockKey1 = "test:lock:multi:1";
        String lockKey2 = "test:lock:multi:2";
        String lockKey3 = "test:lock:multi:3";

        // 获取联锁
        RLock multiLock = lockService.getMultiLock(lockKey1, lockKey2, lockKey3);

        try {
            boolean locked = multiLock.tryLock(5, 10, TimeUnit.SECONDS);
            assertTrue(locked);

            // 验证所有锁都被持有
            assertTrue(multiLock.isHeldByCurrentThread());
        } catch (InterruptedException e) {
            fail("Lock acquisition interrupted");
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    @Test
    void testReadWriteLock() throws InterruptedException {
        String lockKey = "test:lock:readwrite";
        AtomicInteger readCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

        // 创建多个读线程
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                try {
                    if (lockService.tryReadLock(lockKey, 5, 10, TimeUnit.SECONDS)) {
                        try {
                            readCount.incrementAndGet();
                            Thread.sleep(100);
                        } finally {
                            lockService.unlockRead(lockKey);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 创建一个写线程
        new Thread(() -> {
            try {
                Thread.sleep(50); // 让读线程先启动
                if (lockService.tryWriteLock(lockKey, 5, 10, TimeUnit.SECONDS)) {
                    try {
                        // 写操作
                        Thread.sleep(50);
                    } finally {
                        lockService.unlockWrite(lockKey);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }).start();

        latch.await(30, TimeUnit.SECONDS);

        // 验证读操作都执行了
        assertEquals(4, readCount.get());
    }
}
