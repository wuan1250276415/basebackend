package com.basebackend.logging.audit.service;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.crypto.AuditSignatureService;
import com.basebackend.logging.audit.crypto.HashChainCalculator;
import com.basebackend.logging.audit.metrics.AuditMetrics;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.basebackend.logging.audit.storage.AuditStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuditService 增强测试
 * P0优化：测试内存管理和队列溢出处理
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditService 审计服务增强测试")
class AuditServiceEnhancedTest {

    @Mock
    private AuditStorage storage;

    @Mock
    private HashChainCalculator hashChainCalculator;

    @Mock
    private AuditSignatureService signatureService;

    @Mock
    private AuditMetrics metrics;

    private AuditService auditService;

    // 使用较小的队列容量便于测试
    private static final int TEST_QUEUE_CAPACITY = 100;
    private static final int TEST_BATCH_SIZE = 10;
    private static final long TEST_FLUSH_INTERVAL = 100;

    @BeforeEach
    void setUp() {
        when(hashChainCalculator.computeHash(any(), any())).thenReturn("test-hash");
        when(signatureService.needsKeyRotation()).thenReturn(false);
        when(signatureService.sign(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(metrics.isHealthy()).thenReturn(true);

        auditService = new AuditService(
                storage,
                hashChainCalculator,
                signatureService,
                metrics,
                TEST_QUEUE_CAPACITY,
                TEST_BATCH_SIZE,
                TEST_FLUSH_INTERVAL
        );
    }

    @AfterEach
    void tearDown() {
        if (auditService != null) {
            auditService.shutdown();
        }
    }

    @Nested
    @DisplayName("基础功能测试")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("记录审计事件 - 成功场景")
        void shouldRecordAuditEventSuccessfully() {
            Map<String, Object> details = new HashMap<>();
            details.put("action", "login");

            auditService.record(
                    "user123",
                    AuditEventType.LOGIN,
                    "/api/login",
                    "SUCCESS",
                    "192.168.1.1",
                    "Mozilla/5.0",
                    "LOGIN",
                    "entity-1",
                    "session-1",
                    details
            );

            verify(hashChainCalculator, times(1)).computeHash(any(), any());
            verify(signatureService, times(1)).sign(any());
            verify(metrics, times(1)).recordSuccess(anyLong());
        }

        @Test
        @DisplayName("获取队列状态 - 应返回正确状态")
        void shouldGetQueueStatus() {
            AuditService.AuditQueueStatus status = auditService.getQueueStatus();

            assertThat(status).isNotNull();
            assertThat(status.getQueueCapacity()).isEqualTo(TEST_QUEUE_CAPACITY);
            assertThat(status.getCurrentSize()).isGreaterThanOrEqualTo(0);
            assertThat(status.getPercentFull()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("手动刷盘 - 应触发存储写入")
        void shouldFlushManually() throws Exception {
            // 先记录一些事件
            for (int i = 0; i < 5; i++) {
                auditService.record(
                        "user" + i,
                        AuditEventType.API_ACCESS,
                        "/api/data",
                        "SUCCESS",
                        "127.0.0.1",
                        "Test",
                        "READ",
                        "entity-" + i,
                        "session-" + i,
                        null
                );
            }

            // 手动刷盘
            auditService.flush();

            // 等待异步处理
            Thread.sleep(200);

            verify(storage, atLeastOnce()).batchSave(any());
        }

        @Test
        @DisplayName("清空队列 - 应清空所有待处理事件")
        void shouldClearQueue() {
            // 记录一些事件
            for (int i = 0; i < 5; i++) {
                auditService.record(
                        "user" + i,
                        AuditEventType.API_ACCESS,
                        "/api/data",
                        "SUCCESS",
                        "127.0.0.1",
                        "Test",
                        "READ",
                        "entity-" + i,
                        "session-" + i,
                        null
                );
            }

            // 清空队列
            auditService.clearQueue();

            AuditService.AuditQueueStatus status = auditService.getQueueStatus();
            assertThat(status.getCurrentSize()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("队列状态测试")
    class QueueStatusTests {

        @Test
        @DisplayName("队列状态 - 应包含所有字段")
        void shouldContainAllStatusFields() {
            AuditService.AuditQueueStatus status = auditService.getQueueStatus();

            assertThat(status.getCurrentSize()).isGreaterThanOrEqualTo(0);
            assertThat(status.getQueueCapacity()).isEqualTo(TEST_QUEUE_CAPACITY);
            assertThat(status.getPercentFull()).isGreaterThanOrEqualTo(0);
            assertThat(status.getTotalEntries()).isGreaterThanOrEqualTo(0);
            assertThat(status.getDroppedEntries()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("队列状态toString - 应返回格式化字符串")
        void shouldFormatStatusToString() {
            AuditService.AuditQueueStatus status = auditService.getQueueStatus();
            String statusStr = status.toString();

            assertThat(statusStr).contains("队列状态");
            assertThat(statusStr).contains(String.valueOf(TEST_QUEUE_CAPACITY));
        }
    }

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("多线程记录 - 应线程安全")
        void shouldBeThreadSafe() throws InterruptedException {
            int threadCount = 10;
            int eventsPerThread = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < eventsPerThread; i++) {
                            auditService.record(
                                    "user-" + threadId + "-" + i,
                                    AuditEventType.API_ACCESS,
                                    "/api/test",
                                    "SUCCESS",
                                    "127.0.0.1",
                                    "Test",
                                    "READ",
                                    "entity-" + i,
                                    "session-" + threadId,
                                    null
                            );
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(threadCount * eventsPerThread);
        }
    }

    @Nested
    @DisplayName("P0优化：内存管理测试")
    class MemoryManagementTests {

        @Test
        @DisplayName("获取丢弃条目数 - 初始应为0")
        void shouldGetDroppedEntriesInitiallyZero() {
            long dropped = auditService.getDroppedEntries();
            assertThat(dropped).isEqualTo(0);
        }

        @Test
        @DisplayName("队列状态包含丢弃数 - 应正确显示")
        void shouldIncludeDroppedInStatus() {
            AuditService.AuditQueueStatus status = auditService.getQueueStatus();
            assertThat(status.getDroppedEntries()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("关闭测试")
    class ShutdownTests {

        @Test
        @DisplayName("正常关闭 - 应完成最后刷盘")
        void shouldShutdownGracefully() {
            // 记录一些事件
            for (int i = 0; i < 3; i++) {
                auditService.record(
                        "user" + i,
                        AuditEventType.API_ACCESS,
                        "/api/data",
                        "SUCCESS",
                        "127.0.0.1",
                        "Test",
                        "READ",
                        "entity-" + i,
                        "session-" + i,
                        null
                );
            }

            // 关闭服务
            auditService.shutdown();

            // 验证存储被关闭
            verify(storage, times(1)).close();
        }
    }

    @Nested
    @DisplayName("AuditQueueStatus Builder测试")
    class QueueStatusBuilderTests {

        @Test
        @DisplayName("Builder - 应正确构建状态对象")
        void shouldBuildStatusCorrectly() {
            AuditService.AuditQueueStatus status = AuditService.AuditQueueStatus.builder()
                    .currentSize(50)
                    .queueCapacity(100)
                    .percentFull(50)
                    .totalEntries(1000)
                    .lastHash("abc123")
                    .droppedEntries(5)
                    .needsFlush(true)
                    .build();

            assertThat(status.getCurrentSize()).isEqualTo(50);
            assertThat(status.getQueueCapacity()).isEqualTo(100);
            assertThat(status.getPercentFull()).isEqualTo(50);
            assertThat(status.getTotalEntries()).isEqualTo(1000);
            assertThat(status.getLastHash()).isEqualTo("abc123");
            assertThat(status.getDroppedEntries()).isEqualTo(5);
            assertThat(status.isNeedsFlush()).isTrue();
        }
    }
}
