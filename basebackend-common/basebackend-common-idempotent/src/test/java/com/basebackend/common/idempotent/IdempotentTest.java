package com.basebackend.common.idempotent;

import com.basebackend.common.idempotent.config.IdempotentProperties;
import com.basebackend.common.idempotent.exception.IdempotentException;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.idempotent.store.impl.InMemoryIdempotentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等性单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
class IdempotentTest {

    private InMemoryIdempotentStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotentStore();
    }

    @Test
    @DisplayName("PARAM 策略 - 首次请求应通过")
    void testFirstRequestShouldPass() {
        String key = "idempotent:param:test#method:user1:abc123";
        boolean result = store.tryAcquire(key, 5, TimeUnit.SECONDS);
        assertTrue(result, "首次请求应当通过幂等检查");
    }

    @Test
    @DisplayName("PARAM 策略 - 重复请求应被拒绝")
    void testDuplicateRequestShouldBeRejected() {
        String key = "idempotent:param:test#method:user1:abc123";
        assertTrue(store.tryAcquire(key, 5, TimeUnit.SECONDS));
        assertFalse(store.tryAcquire(key, 5, TimeUnit.SECONDS), "重复请求应被拒绝");
    }

    @Test
    @DisplayName("超时后可重新提交")
    void testRetryAfterTimeout() throws InterruptedException {
        String key = "idempotent:timeout:test";
        assertTrue(store.tryAcquire(key, 100, TimeUnit.MILLISECONDS));
        assertFalse(store.tryAcquire(key, 100, TimeUnit.MILLISECONDS));

        // 等待超时
        Thread.sleep(150);

        assertTrue(store.tryAcquire(key, 100, TimeUnit.MILLISECONDS),
                "超时后应当可以重新提交");
    }

    @Test
    @DisplayName("内存存储 - 手动释放后可重新提交")
    void testReleaseAllowsResubmit() {
        String key = "idempotent:release:test";
        assertTrue(store.tryAcquire(key, 60, TimeUnit.SECONDS));
        assertFalse(store.tryAcquire(key, 60, TimeUnit.SECONDS));

        store.release(key);
        assertTrue(store.tryAcquire(key, 60, TimeUnit.SECONDS),
                "释放后应当可以重新提交");
    }

    @Test
    @DisplayName("IdempotentException 构造")
    void testIdempotentException() {
        IdempotentException ex = new IdempotentException("请勿重复提交");
        assertEquals("请勿重复提交", ex.getMessage());
        assertEquals(1022, ex.getCode());
    }

    @Test
    @DisplayName("IdempotentProperties 默认值")
    void testPropertiesDefaults() {
        IdempotentProperties props = new IdempotentProperties();
        assertTrue(props.isEnabled());
        assertEquals(5, props.getDefaultTimeout());
        assertEquals(TimeUnit.SECONDS, props.getDefaultTimeUnit());
        assertEquals("X-Idempotent-Token", props.getTokenHeader());
        assertEquals(300, props.getTokenTimeout());
    }

    @Test
    @DisplayName("不同 key 互不影响")
    void testDifferentKeysAreIndependent() {
        String key1 = "idempotent:key1";
        String key2 = "idempotent:key2";

        assertTrue(store.tryAcquire(key1, 5, TimeUnit.SECONDS));
        assertTrue(store.tryAcquire(key2, 5, TimeUnit.SECONDS),
                "不同 key 应当互不影响");
    }
}
