package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.config.BackupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Redisson分布式锁管理器测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RedissonLockManager 分布式锁管理器测试")
class RedissonLockManagerTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private BackupProperties.DistributedLock distributedLockConfig;

    @Mock
    private RLock mockLock;

    private RedissonLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new RedissonLockManager(redissonClient,backupProperties);

        // 使用反射设置private字段
        setField(lockManager, "redissonClient", redissonClient);
        setField(lockManager, "backupProperties", backupProperties);

        when(backupProperties.getDistributedLock()).thenReturn(distributedLockConfig);
        when(distributedLockConfig.getWaitTime()).thenReturn(java.time.Duration.ofSeconds(10));
        when(distributedLockConfig.getTtl()).thenReturn(java.time.Duration.ofSeconds(30));
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);
    }

    /**
     * 使用反射设置private字段
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Test
    @DisplayName("成功执行带锁的Runnable操作")
    void shouldExecuteRunnableWithLockSuccessfully() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);

        // When
        lockManager.withLock("test-lock", action);

        // Then
        assertThat(executed.get()).isTrue();
        verify(mockLock, times(1)).tryLock(10000, 30000, TimeUnit.MILLISECONDS);
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("成功执行带锁的Callable操作")
    void shouldExecuteCallableWithLockSuccessfully() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        Callable<String> action = () -> "test-result";

        // When
        String result = lockManager.withLock("test-lock", action);

        // Then
        assertThat(result).isEqualTo("test-result");
        verify(mockLock, times(1)).tryLock(10000, 30000, TimeUnit.MILLISECONDS);
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("获取锁失败时抛出异常")
    void shouldThrowExceptionWhenLockAcquisitionFails() throws Exception {
        // Given - 模拟获取锁失败
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        Runnable action = () -> {};

        // When & Then
        assertThatThrownBy(() -> lockManager.withLock("test-lock", action))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("获取分布式锁失败")
                .hasMessageContaining("等待超时");

        verify(mockLock, never()).unlock();
    }

    @Test
    @DisplayName("获取锁被中断时抛出异常")
    void shouldThrowExceptionWhenLockAcquisitionIsInterrupted() throws Exception {
        // Given - 模拟获取锁时被中断
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenThrow(new InterruptedException("Thread interrupted"));

        Runnable action = () -> {};

        // When & Then
        assertThatThrownBy(() -> lockManager.withLock("test-lock", action))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("获取分布式锁被中断")
                .hasMessageContaining("test-lock");

        verify(mockLock, never()).unlock();
    }

    @Test
    @DisplayName("操作执行异常时确保锁被释放")
    void shouldEnsureLockReleasedWhenActionThrowsException() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        RuntimeException testException = new RuntimeException("Test exception");
        Runnable action = () -> {
            throw testException;
        };

        // When & Then
        assertThatThrownBy(() -> lockManager.withLock("test-lock", action))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        // 验证即使操作抛出异常，锁仍被释放
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("使用自定义超时时间执行带锁操作")
    void shouldExecuteWithCustomTimeout() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);

        // When
        lockManager.withLock("test-lock", 5000, 15000, action);

        // Then
        assertThat(executed.get()).isTrue();
        verify(mockLock, times(1)).tryLock(5000, 15000, TimeUnit.MILLISECONDS);
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("尝试获取锁（不阻塞）成功")
    void shouldTryLockSuccessfully() throws InterruptedException {
        // Given
        when(mockLock.tryLock(eq(0L), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        // When
        boolean result = lockManager.tryLock("test-lock");

        // Then
        assertThat(result).isTrue();
        verify(mockLock, times(1)).tryLock(0, 30000, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("尝试获取锁（不阻塞）失败")
    void shouldFailToTryLock() throws InterruptedException {
        // Given
        when(mockLock.tryLock(eq(0L), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        // When
        boolean result = lockManager.tryLock("test-lock");

        // Then
        assertThat(result).isFalse();
        verify(mockLock, times(1)).tryLock(0, 30000, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("尝试获取锁带超时时间成功")
    void shouldTryLockWithTimeoutSuccessfully() throws InterruptedException {
        // Given
        when(mockLock.tryLock(eq(5000L), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        // When
        boolean result = lockManager.tryLock("test-lock", 5000);

        // Then
        assertThat(result).isTrue();
        verify(mockLock, times(1)).tryLock(5000, 30000, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("尝试获取锁带超时时间失败")
    void shouldFailToTryLockWithTimeout() throws InterruptedException {
        // Given
        when(mockLock.tryLock(eq(5000L), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        // When
        boolean result = lockManager.tryLock("test-lock", 5000);

        // Then
        assertThat(result).isFalse();
        verify(mockLock, times(1)).tryLock(5000, 30000, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("尝试获取锁被中断")
    void shouldHandleInterruptedExceptionInTryLock() throws InterruptedException {
        // Given
        when(mockLock.tryLock(eq(5000L), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenThrow(new InterruptedException("Interrupted"));

        // When
        boolean result = lockManager.tryLock("test-lock", 5000);

        // Then
        assertThat(result).isFalse();
        verify(mockLock, times(1)).tryLock(5000, 30000, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("手动释放持有的锁")
    void shouldUnlockHeldLock() {
        // Given
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        // When
        lockManager.unlock("test-lock");

        // Then
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("手动释放未持有的锁记录警告")
    void shouldWarnWhenUnlockingNotHeldLock() {
        // Given
        when(mockLock.isHeldByCurrentThread()).thenReturn(false);

        // When
        lockManager.unlock("test-lock");

        // Then
        verify(mockLock, never()).unlock();
        // 注意：实际测试中需要验证日志输出
    }

    @Test
    @DisplayName("手动释放锁时处理IllegalMonitorStateException")
    void shouldHandleIllegalMonitorStateExceptionOnUnlock() {
        // Given
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalMonitorStateException("Lock not held")).when(mockLock).unlock();

        // When
        lockManager.unlock("test-lock");

        // Then - 不应该抛出异常
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("检查锁是否被当前线程持有")
    void shouldCheckIfLockHeldByCurrentThread() {
        // Given
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        // When
        boolean held = lockManager.isHeldByCurrentThread("test-lock");

        // Then
        assertThat(held).isTrue();
        verify(mockLock, times(1)).isHeldByCurrentThread();
    }

    @Test
    @DisplayName("检查锁是否被任何线程持有")
    void shouldCheckIfLockIsLocked() {
        // Given
        when(mockLock.isLocked()).thenReturn(true);

        // When
        boolean locked = lockManager.isLocked("test-lock");

        // Then
        assertThat(locked).isTrue();
        verify(mockLock, times(1)).isLocked();
    }

    @Test
    @DisplayName("使用空锁键执行操作")
    void shouldHandleEmptyLockKey() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);

        // When
        lockManager.withLock("", action);

        // Then - 空锁键应该仍然能工作（虽然不推荐）
        assertThat(executed.get()).isTrue();
        verify(redissonClient).getLock("");
    }

    @Test
    @DisplayName("使用null锁键执行操作")
    void shouldHandleNullLockKey() throws Exception {
        // Given - mock getLock返回值以避免NullPointer
        RLock nullLock = mock(RLock.class);
        when(redissonClient.getLock(null)).thenReturn(nullLock);
        when(nullLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(nullLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);

        // When
        lockManager.withLock(null, action);

        // Then
        assertThat(executed.get()).isTrue();
        verify(redissonClient).getLock(null);
    }

    @Test
    @DisplayName("幂等性：同一操作重复执行")
    void shouldHandleMultipleLockExecutions() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicInteger executionCount = new AtomicInteger(0);
        Runnable action = executionCount::incrementAndGet;

        // When
        lockManager.withLock("test-lock", action);
        lockManager.withLock("test-lock", action);

        // Then
        assertThat(executionCount.get()).isEqualTo(2);
        verify(mockLock, times(2)).unlock();
    }

    @Test
    @DisplayName("Callable操作返回null")
    void shouldHandleNullReturnValueFromCallable() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        Callable<String> action = () -> null;

        // When
        String result = lockManager.withLock("test-lock", action);

        // Then
        assertThat(result).isNull();
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("带超时的自定义参数验证")
    void shouldValidateCustomTimeoutParameters() throws Exception {
        // Given
        when(mockLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);

        // When - 使用不同的超时值
        lockManager.withLock("test-lock", 100, 200, action);

        // Then
        assertThat(executed.get()).isTrue();
        verify(mockLock, times(1)).tryLock(100, 200, TimeUnit.MILLISECONDS);
        verify(mockLock, times(1)).unlock();
    }
}
