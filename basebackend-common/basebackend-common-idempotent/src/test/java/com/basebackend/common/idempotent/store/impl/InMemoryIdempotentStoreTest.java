package com.basebackend.common.idempotent.store.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryIdempotentStore 单元测试
 */
class InMemoryIdempotentStoreTest {

    private InMemoryIdempotentStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotentStore();
    }

    // ========== tryAcquire ==========

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        @DisplayName("首次获取成功")
        void shouldAcquireFirst() {
            assertThat(store.tryAcquire("order-001", 5, TimeUnit.SECONDS)).isTrue();
        }

        @Test
        @DisplayName("重复获取失败（幂等拦截）")
        void shouldRejectDuplicate() {
            assertThat(store.tryAcquire("order-002", 5, TimeUnit.SECONDS)).isTrue();
            assertThat(store.tryAcquire("order-002", 5, TimeUnit.SECONDS)).isFalse();
        }

        @Test
        @DisplayName("不同 key 互不影响")
        void shouldNotInterfereWithDifferentKeys() {
            assertThat(store.tryAcquire("key-a", 5, TimeUnit.SECONDS)).isTrue();
            assertThat(store.tryAcquire("key-b", 5, TimeUnit.SECONDS)).isTrue();
        }

        @Test
        @DisplayName("过期后可重新获取")
        void shouldAllowReacquireAfterExpiry() throws Exception {
            assertThat(store.tryAcquire("expire-test", 50, TimeUnit.MILLISECONDS)).isTrue();
            Thread.sleep(100);
            assertThat(store.tryAcquire("expire-test", 50, TimeUnit.MILLISECONDS)).isTrue();
        }

        @Test
        @DisplayName("并发场景只有一个线程获取成功")
        void shouldAllowOnlyOneInConcurrency() throws Exception {
            int threads = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threads);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threads; i++) {
                Thread.ofVirtual().start(() -> {
                    try {
                        startLatch.await();
                        if (store.tryAcquire("concurrent-key", 5, TimeUnit.SECONDS)) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);

            assertThat(successCount.get()).isEqualTo(1);
        }
    }

    // ========== release ==========

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("释放后可重新获取")
        void shouldAllowReacquireAfterRelease() {
            store.tryAcquire("release-test", 5, TimeUnit.SECONDS);
            store.release("release-test");
            assertThat(store.tryAcquire("release-test", 5, TimeUnit.SECONDS)).isTrue();
        }

        @Test
        @DisplayName("释放不存在的 key 不抛异常")
        void shouldNotThrowForNonExistentKey() {
            store.release("non-existent"); // should not throw
        }
    }
}
