package com.basebackend.common.lock.provider.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryDistributedLockProvider 单元测试
 */
class InMemoryDistributedLockProviderTest {

    private InMemoryDistributedLockProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InMemoryDistributedLockProvider();
    }

    // ========== tryLock ==========

    @Nested
    @DisplayName("tryLock")
    class TryLock {

        @Test
        @DisplayName("正常加锁成功")
        void shouldAcquireLock() {
            boolean locked = provider.tryLock("test-key", 0, 10, TimeUnit.SECONDS);
            assertThat(locked).isTrue();
            assertThat(provider.isLocked("test-key")).isTrue();
            provider.unlock("test-key");
        }

        @Test
        @DisplayName("同一线程可重入")
        void shouldBeReentrant() {
            assertThat(provider.tryLock("reentrant", 0, 10, TimeUnit.SECONDS)).isTrue();
            assertThat(provider.tryLock("reentrant", 0, 10, TimeUnit.SECONDS)).isTrue();
            provider.unlock("reentrant");
            // 重入锁需要解锁两次
            assertThat(provider.isLocked("reentrant")).isTrue();
            provider.unlock("reentrant");
        }

        @Test
        @DisplayName("不同 key 互不影响")
        void shouldNotInterfereWithDifferentKeys() {
            assertThat(provider.tryLock("key-a", 0, 10, TimeUnit.SECONDS)).isTrue();
            assertThat(provider.tryLock("key-b", 0, 10, TimeUnit.SECONDS)).isTrue();
            provider.unlock("key-a");
            provider.unlock("key-b");
        }

        @Test
        @DisplayName("另一线程加锁超时失败")
        void shouldFailWhenLockedByOtherThread() throws Exception {
            provider.tryLock("contested", 0, 10, TimeUnit.SECONDS);

            AtomicBoolean otherResult = new AtomicBoolean(true);
            CountDownLatch latch = new CountDownLatch(1);
            Thread other = Thread.ofVirtual().start(() -> {
                otherResult.set(provider.tryLock("contested", 50, 10, TimeUnit.MILLISECONDS));
                latch.countDown();
            });
            latch.await(2, TimeUnit.SECONDS);

            assertThat(otherResult.get()).isFalse();
            provider.unlock("contested");
        }
    }

    // ========== unlock ==========

    @Nested
    @DisplayName("unlock")
    class Unlock {

        @Test
        @DisplayName("正常解锁")
        void shouldUnlock() {
            provider.tryLock("unlock-test", 0, 10, TimeUnit.SECONDS);
            provider.unlock("unlock-test");
            assertThat(provider.isLocked("unlock-test")).isFalse();
        }

        @Test
        @DisplayName("解锁不存在的 key 不抛异常")
        void shouldNotThrowForNonExistentKey() {
            provider.unlock("non-existent");
            // no exception
        }

        @Test
        @DisplayName("解锁后其他线程可以获取锁")
        void shouldAllowOtherThreadAfterUnlock() throws Exception {
            provider.tryLock("release-test", 0, 10, TimeUnit.SECONDS);
            provider.unlock("release-test");

            AtomicBoolean otherResult = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);
            Thread.ofVirtual().start(() -> {
                otherResult.set(provider.tryLock("release-test", 100, 10, TimeUnit.MILLISECONDS));
                if (otherResult.get()) provider.unlock("release-test");
                latch.countDown();
            });
            latch.await(2, TimeUnit.SECONDS);
            assertThat(otherResult.get()).isTrue();
        }
    }

    // ========== isLocked ==========

    @Nested
    @DisplayName("isLocked")
    class IsLocked {

        @Test
        @DisplayName("未加锁返回 false")
        void shouldReturnFalseWhenNotLocked() {
            assertThat(provider.isLocked("no-lock")).isFalse();
        }

        @Test
        @DisplayName("加锁后返回 true")
        void shouldReturnTrueWhenLocked() {
            provider.tryLock("locked", 0, 10, TimeUnit.SECONDS);
            assertThat(provider.isLocked("locked")).isTrue();
            provider.unlock("locked");
        }
    }

    // ========== forceUnlock ==========

    @Nested
    @DisplayName("forceUnlock")
    class ForceUnlock {

        @Test
        @DisplayName("强制解锁")
        void shouldForceUnlock() {
            provider.tryLock("force", 0, 10, TimeUnit.SECONDS);
            provider.tryLock("force", 0, 10, TimeUnit.SECONDS); // 重入
            provider.forceUnlock("force");
            assertThat(provider.isLocked("force")).isFalse();
        }

        @Test
        @DisplayName("强制解锁不存在的 key 不抛异常")
        void shouldNotThrowForNonExistentKey() {
            provider.forceUnlock("non-existent");
            // no exception
        }
    }
}
