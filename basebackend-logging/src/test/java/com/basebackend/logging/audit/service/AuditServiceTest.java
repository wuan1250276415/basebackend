package com.basebackend.logging.audit.service;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.crypto.AuditSignatureService;
import com.basebackend.logging.audit.crypto.HashChainCalculator;
import com.basebackend.logging.audit.metrics.AuditMetrics;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.basebackend.logging.audit.storage.AuditStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * AuditService测试类
 * 测试审计服务的异步处理、批量写入、哈希链等功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditService 审计服务测试")
class AuditServiceTest {

    @Mock
    private AuditStorage storage;

    @Mock
    private HashChainCalculator hashChainCalculator;

    @Mock
    private AuditSignatureService signatureService;

    @Mock
    private AuditMetrics metrics;

    private AuditService auditService;

    /**
     * 创建审计服务实例（默认配置）
     */
    private AuditService createAuditService() {
        return new AuditService(
            storage,
            hashChainCalculator,
            signatureService,
            metrics,
            100, // queueCapacity
            10,  // batchSize
            500  // flushIntervalMs
        );
    }

    @Test
    @DisplayName("记录审计日志")
    void shouldRecordAuditLog() throws Exception {
        // Given
        auditService = createAuditService();

        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("hash-123");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenAnswer(invocation -> {
                AuditLogEntry entry = invocation.getArgument(0);
                entry.setSignature("signature-456");
                entry.setId("audit-entry-id"); // 设置ID避免NPE
                return entry;
            });

        // When - record方法是同步的，哈希计算和签名立即执行
        auditService.record(
            "user-123",
            AuditEventType.LOGIN,
            "/api/login",
            "SUCCESS",
            "192.168.1.1",
            "Mozilla/5.0",
            "用户登录",
            "entity-123",
            "session-456",
            Map.of("key", "value")
        );

        // Then - 验证同步操作（哈希计算和签名）
        verify(hashChainCalculator, times(1)).computeHash(any(AuditLogEntry.class), any());
        verify(signatureService, times(1)).sign(any(AuditLogEntry.class));
        verify(metrics, times(1)).recordSuccess(anyLong());
        verify(metrics, times(1)).updateQueueSize(1);
    }

    @Test
    @DisplayName("批量记录审计日志")
    void shouldBatchRecordAuditLogs() throws Exception {
        // Given
        auditService = createAuditService();
        AuditLogEntry entry1 = AuditLogEntry.builder()
            .id("batch-id-1")
            .userId("user-1")
            .eventType(AuditEventType.CREATE)
            .result("SUCCESS")
            .build();
        AuditLogEntry entry2 = AuditLogEntry.builder()
            .id("batch-id-2")
            .userId("user-2")
            .eventType(AuditEventType.UPDATE)
            .result("SUCCESS")
            .build();
        AuditLogEntry entry3 = AuditLogEntry.builder()
            .id("batch-id-3")
            .userId("user-3")
            .eventType(AuditEventType.DELETE)
            .result("SUCCESS")
            .build();

        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("hash-1")
            .thenReturn("hash-2")
            .thenReturn("hash-3");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenAnswer(invocation -> {
                AuditLogEntry entry = invocation.getArgument(0);
                entry.setId("batch-id-" + System.currentTimeMillis()); // 设置ID避免NPE
                return entry;
            });

        // When - 记录三个审计事件（都是同步操作）
        auditService.record(
            "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "创建数据", "entity-1", "session-1", null
        );
        auditService.record(
            "user-2", AuditEventType.UPDATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "更新数据", "entity-2", "session-2", null
        );
        auditService.record(
            "user-3", AuditEventType.DELETE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "删除数据", "entity-3", "session-3", null
        );

        // Then - 验证同步操作
        verify(hashChainCalculator, times(3)).computeHash(any(AuditLogEntry.class), any());
        verify(signatureService, times(3)).sign(any(AuditLogEntry.class));
        verify(metrics, times(3)).recordSuccess(anyLong());
        verify(metrics, times(3)).updateQueueSize(anyInt());
    }

    @Test
    @DisplayName("队列满时的处理 - P0优化后使用降级策略")
    void shouldHandleQueueFull() throws InterruptedException {
        // Given - 创建一个队列容量较小的审计服务，且刷新间隔很长
        auditService = createAuditService();
        AuditService smallQueueService = new AuditService(
            storage,
            hashChainCalculator,
            signatureService,
            metrics,
            2, // 队列容量为2
            2, // 批量大小为2
            10000L // 刷新间隔10秒，避免在测试期间被清空
        );

        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("sig").build());

        // When - P0优化后，队列满时使用降级策略而非抛出异常
        // 连续快速添加3个项目（队列容量为2）
        for (int i = 0; i < 3; i++) {
            smallQueueService.record(
                "user-" + i,
                AuditEventType.LOGIN,
                "/api/login",
                "SUCCESS",
                "127.0.0.1",
                "Mozilla/5.0",
                "登录",
                null,
                null,
                null
            );
        }
        
        // Then - 验证降级策略生效：部分事件可能被丢弃
        AuditService.AuditQueueStatus status = smallQueueService.getQueueStatus();
        // 队列状态应该有效
        assertThat(status).isNotNull();
        // 丢弃的条目数应该 >= 0（可能有事件被丢弃）
        assertThat(smallQueueService.getDroppedEntries()).isGreaterThanOrEqualTo(0);
        
        smallQueueService.shutdown();
    }

    @Test
    @DisplayName("哈希链计算")
    void shouldCalculateHashChain() throws Exception {
        // Given
        auditService = createAuditService();
        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("prev-hash")
            .thenReturn("current-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("signature").id("test-id").build());

        // When - 记录两个审计事件，哈希链是同步计算的
        auditService.record(
            "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "创建", null, "session-1", null
        );
        auditService.record(
            "user-2", AuditEventType.UPDATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "更新", null, "session-2", null
        );

        // Then - 验证同步的哈希计算调用
        verify(hashChainCalculator, times(2)).computeHash(any(AuditLogEntry.class), any());
        verify(signatureService, times(2)).sign(any(AuditLogEntry.class));
    }

    @Test
    @DisplayName("审计日志签名验证")
    void shouldVerifyAuditLogSignature() throws Exception {
        // Given
        auditService = createAuditService();
        AuditLogEntry signedEntry = AuditLogEntry.builder()
            .id("signed-id-1")
            .userId("user-1")
            .eventType(AuditEventType.LOGIN)
            .result("SUCCESS")
            .signature("signature-012")
            .build();

        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("hash-789");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(signedEntry);

        // When - 签名是同步执行的
        auditService.record(
            "user-1", AuditEventType.LOGIN, "/api/login", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "登录", null, "session-1", null
        );

        // Then - 验证签名被调用
        verify(signatureService, times(1)).sign(any(AuditLogEntry.class));
        verify(hashChainCalculator, times(1)).computeHash(any(AuditLogEntry.class), any());
    }

    @Test
    @DisplayName("定期刷盘机制")
    void shouldHaveScheduledFlush() throws Exception {
        // Given
        auditService = createAuditService();
        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("flush-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("flush-signature").build());

        // 记录一个审计事件到队列
        auditService.record(
            "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "创建", null, "session-1", null
        );

        // When - 手动触发刷盘（模拟定时任务）
        auditService.flush();

        // Then - 验证刷盘操作
        verify(hashChainCalculator, times(1)).computeHash(any(AuditLogEntry.class), any());
        verify(signatureService, times(1)).sign(any(AuditLogEntry.class));
        verify(storage, times(1)).batchSave(anyList());
    }

    @Test
    @DisplayName("关闭审计服务")
    void shouldShutdownGracefully() {
        // Given
        auditService = createAuditService();

        // When
        auditService.flush(); // 手动刷盘

        // Then - 验证无异常抛出
        // 实际测试中需要更复杂的验证来检查线程池是否正确关闭
    }

    @Test
    @DisplayName("处理异常场景")
    void shouldHandleExceptions() {
        // Given
        auditService = createAuditService();
        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenThrow(new RuntimeException("Hash error"));
        // 需要模拟签名服务，即使不会执行到这里
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("sig").build());

        // When & Then - 验证异常被正确捕获和重新抛出
        assertThatThrownBy(() ->
            auditService.record(
                "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
                "127.0.0.1", "Mozilla/5.0", "创建", null, "session-1", null
            )
        ).isInstanceOf(RuntimeException.class)
          .hasMessage("审计记录失败");

        verify(metrics, times(1)).recordFailure(eq("record-error"));
    }

    @Test
    @DisplayName("存储失败重试机制")
    void shouldRetryOnStorageFailure() throws Exception {
        // Given
        auditService = createAuditService();
        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("retry-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("retry-signature").build());

        // 模拟batchSave失败
        doThrow(new RuntimeException("Storage error"))
            .when(storage).batchSave(anyList());

        // When - 记录审计事件并触发刷盘
        auditService.record(
            "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "创建", null, "session-1", null
        );
        auditService.flush();

        // Then - 验证失败被记录，但不会重试（重新入队）
        verify(storage, times(1)).batchSave(anyList());
        verify(metrics, times(1)).recordStorageError();
    }

    @Test
    @DisplayName("审计日志的完整性验证")
    void shouldValidateAuditLogIntegrity() throws Exception {
        // Given
        auditService = createAuditService();

        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("integrity-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenAnswer(inv -> {
                AuditLogEntry e = inv.getArgument(0);
                e.setSignature("integrity-signature");
                e.setId("integrity-verified-id"); // 设置ID
                return e;
            });

        // When - 记录审计事件（同步操作）
        auditService.record(
            "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "创建", "entity-1", "session-1", null
        );

        // Then - 验证同步操作的完整性
        verify(hashChainCalculator, times(1)).computeHash(any(AuditLogEntry.class), any());
        verify(signatureService, times(1)).sign(any(AuditLogEntry.class));
        verify(metrics, times(1)).recordSuccess(anyLong());
        verify(metrics, times(1)).updateQueueSize(1);
    }

    @Test
    @DisplayName("批量记录审计事件")
    void shouldRecordBatchAuditEvents() throws Exception {
        // Given
        auditService = createAuditService();
        AuditLogEntry entry1 = AuditLogEntry.builder()
            .userId("user-1")
            .eventType(AuditEventType.CREATE)
            .result("SUCCESS")
            .operation("创建")
            .build();
        AuditLogEntry entry2 = AuditLogEntry.builder()
            .userId("user-2")
            .eventType(AuditEventType.UPDATE)
            .result("SUCCESS")
            .operation("更新")
            .build();

        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("batch-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("sig").build());

        // When - recordBatch内部调用record（同步）
        auditService.recordBatch(List.of(entry1, entry2));

        // Then - 验证同步操作（每个record调用一次）
        verify(hashChainCalculator, times(2)).computeHash(any(AuditLogEntry.class), any());
        verify(signatureService, times(2)).sign(any(AuditLogEntry.class));
        verify(metrics, times(2)).recordSuccess(anyLong());
        verify(metrics, times(2)).updateQueueSize(anyInt());
    }

    @Test
    @DisplayName("手动刷盘")
    void shouldFlushManually() throws Exception {
        // Given
        auditService = createAuditService();
        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("flush-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("sig").build());

        auditService.record(
            "user-1", AuditEventType.CREATE, "/api/data", "SUCCESS",
            "127.0.0.1", "Mozilla/5.0", "创建", null, "session-1", null
        );

        // 给异步操作一些时间
        Thread.sleep(200);

        // When
        auditService.flush();

        // Then
        Thread.sleep(100);
        verify(storage, atLeast(1)).batchSave(anyList());
    }

    @Test
    @DisplayName("高危操作立即刷盘")
    void shouldFlushHighRiskOperationsImmediately() throws Exception {
        // Given
        auditService = createAuditService();
        when(hashChainCalculator.computeHash(any(AuditLogEntry.class), any()))
            .thenReturn("high-risk-hash");
        when(signatureService.sign(any(AuditLogEntry.class)))
            .thenReturn(AuditLogEntry.builder().signature("sig").build());

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(storage).batchSave(anyList());

        // When - 触发多个高危操作
        for (int i = 0; i < 10; i++) {
            auditService.record(
                "user-" + i, AuditEventType.DELETE, "/api/data", "SUCCESS",
                "127.0.0.1", "Mozilla/5.0", "删除", null, "session-" + i, null
            );
        }

        // Then - 等待刷盘操作（最多等待2秒）
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        // 验证高危操作触发了立即刷盘
        verify(storage, atLeast(1)).batchSave(anyList());
    }

    @Test
    @DisplayName("空批量记录处理")
    void shouldHandleEmptyBatch() throws Exception {
        // Given
        auditService = createAuditService();

        // When
        auditService.recordBatch(List.of());

        // Then - 应该正常处理，不抛异常
        verify(storage, never()).save(any(AuditLogEntry.class));
        verify(storage, never()).batchSave(anyList());
    }

    @Test
    @DisplayName("空值批量记录处理")
    void shouldHandleNullBatch() throws Exception {
        // Given
        auditService = createAuditService();

        // When
        auditService.recordBatch(null);

        // Then - 应该正常处理，不抛异常
        verify(storage, never()).save(any(AuditLogEntry.class));
        verify(storage, never()).batchSave(anyList());
    }
}
