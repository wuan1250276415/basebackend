package com.basebackend.messaging.idempotency;

import com.basebackend.messaging.config.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IdempotencyService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("幂等性服务测试")
class IdempotencyServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private MessagingProperties properties;

    @Mock
    @SuppressWarnings("rawtypes")
    private RBucket bucket;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        // 配置属性
        MessagingProperties.Idempotency idempotencyProps = new MessagingProperties.Idempotency();
        idempotencyProps.setKeyPrefix("msg:idempotency:");
        idempotencyProps.setExpireTime(86400L);
        lenient().when(properties.getIdempotency()).thenReturn(idempotencyProps);

        idempotencyService = new IdempotencyService(redissonClient, properties);
    }

    @Nested
    @DisplayName("重复检查测试")
    class DuplicateCheckTests {

        @Test
        @DisplayName("检查消息是否重复 - 已处理")
        @SuppressWarnings("unchecked")
        void testIsDuplicate_True() {
            // Arrange
            when(redissonClient.getBucket("msg:idempotency:msg-001")).thenReturn(bucket);
            when(bucket.isExists()).thenReturn(true);

            // Act
            boolean result = idempotencyService.isDuplicate("msg-001");

            // Assert
            assertTrue(result);
            verify(redissonClient).getBucket("msg:idempotency:msg-001");
        }

        @Test
        @DisplayName("检查消息是否重复 - 未处理")
        @SuppressWarnings("unchecked")
        void testIsDuplicate_False() {
            // Arrange
            when(redissonClient.getBucket("msg:idempotency:msg-002")).thenReturn(bucket);
            when(bucket.isExists()).thenReturn(false);

            // Act
            boolean result = idempotencyService.isDuplicate("msg-002");

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("标记处理测试")
    class MarkProcessedTests {

        @Test
        @DisplayName("标记消息已处理")
        @SuppressWarnings("unchecked")
        void testMarkAsProcessed() {
            // Arrange
            when(redissonClient.getBucket("msg:idempotency:msg-001")).thenReturn(bucket);
            doNothing().when(bucket).set(anyString(), any(Duration.class));

            // Act
            idempotencyService.markAsProcessed("msg-001");

            // Assert
            verify(bucket).set(eq("1"), eq(Duration.ofSeconds(86400L)));
        }
    }

    @Nested
    @DisplayName("锁操作测试")
    class LockTests {

        @Test
        @DisplayName("尝试获取锁 - 成功")
        @SuppressWarnings("unchecked")
        void testTryLock_Success() {
            // Arrange
            when(redissonClient.getBucket("msg:idempotency:msg-001:lock")).thenReturn(bucket);
            when(bucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(true);

            // Act
            boolean result = idempotencyService.tryLock("msg-001");

            // Assert
            assertTrue(result);
            verify(bucket).setIfAbsent(eq("1"), eq(Duration.ofSeconds(60)));
        }

        @Test
        @DisplayName("尝试获取锁 - 失败（已被占用）")
        @SuppressWarnings("unchecked")
        void testTryLock_Failed() {
            // Arrange
            when(redissonClient.getBucket("msg:idempotency:msg-001:lock")).thenReturn(bucket);
            when(bucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(false);

            // Act
            boolean result = idempotencyService.tryLock("msg-001");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("释放锁")
        @SuppressWarnings("unchecked")
        void testUnlock() {
            // Arrange
            when(redissonClient.getBucket("msg:idempotency:msg-001:lock")).thenReturn(bucket);
            when(bucket.delete()).thenReturn(true);

            // Act
            idempotencyService.unlock("msg-001");

            // Assert
            verify(bucket).delete();
        }
    }
}
