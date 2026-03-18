package com.basebackend.cache.lock;

import com.basebackend.cache.exception.CacheLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceImplTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock firstLock;

    @Mock
    private RLock secondLock;

    @Mock
    private RLock fairLock;

    @Mock
    private RLock multiLock;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void getFairLockDelegatesToRedissonClient() {
        when(redissonClient.getFairLock("fair:order")).thenReturn(fairLock);

        DistributedLockServiceImpl service = new DistributedLockServiceImpl(redissonClient);

        assertSame(fairLock, service.getFairLock("fair:order"));
        verify(redissonClient).getFairLock("fair:order");
    }

    @Test
    void getMultiLockDelegatesUsingAllKeys() {
        when(redissonClient.getLock("lock:1")).thenReturn(firstLock);
        when(redissonClient.getLock("lock:2")).thenReturn(secondLock);
        when(redissonClient.getMultiLock(firstLock, secondLock)).thenReturn(multiLock);

        DistributedLockServiceImpl service = new DistributedLockServiceImpl(redissonClient);

        assertSame(multiLock, service.getMultiLock("lock:1", "lock:2"));
        verify(redissonClient).getLock("lock:1");
        verify(redissonClient).getLock("lock:2");
        verify(redissonClient).getMultiLock(firstLock, secondLock);
    }

    @Test
    void getMultiLockRejectsEmptyKeys() {
        DistributedLockServiceImpl service = new DistributedLockServiceImpl(redissonClient);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getMultiLock());

        assertEquals("Lock keys cannot be null or empty", exception.getMessage());
    }

    @Test
    void tryLockRestoresInterruptFlagWhenInterrupted() throws InterruptedException {
        when(redissonClient.getLock("order:1")).thenReturn(firstLock);
        when(firstLock.tryLock(1, 5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        DistributedLockServiceImpl service = new DistributedLockServiceImpl(redissonClient);

        CacheLockException exception = assertThrows(CacheLockException.class,
                () -> service.tryLock("order:1", 1, 5, TimeUnit.SECONDS));

        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(exception.getMessage().contains("order:1"));
    }

    @Test
    void unlockSkipsLockNotHeldByCurrentThread() {
        when(redissonClient.getLock("order:2")).thenReturn(firstLock);
        when(firstLock.isHeldByCurrentThread()).thenReturn(false);

        DistributedLockServiceImpl service = new DistributedLockServiceImpl(redissonClient);
        service.unlock("order:2");

        verify(firstLock, never()).unlock();
    }
}
