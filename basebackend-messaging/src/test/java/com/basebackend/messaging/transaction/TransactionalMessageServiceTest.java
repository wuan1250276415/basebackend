package com.basebackend.messaging.transaction;

import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.model.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionalMessageService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("事务消息服务测试")
class TransactionalMessageServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TransactionalMessageService transactionalMessageService;

    private Message<String> testMessage;

    @BeforeEach
    void setUp() {
        testMessage = Message.<String>builder()
                .messageId("msg-001")
                .topic("test-topic")
                .routingKey("test.routing.key")
                .tags("test-tag")
                .payload("test payload")
                .headers(Map.of("key1", "value1"))
                .sendTime(LocalDateTime.now())
                .delayMillis(0L)
                .retryCount(0)
                .maxRetries(3)
                .partitionKey("partition-001")
                .build();
    }

    @Nested
    @DisplayName("保存消息测试")
    class SaveMessageTests {

        @Test
        @DisplayName("保存消息到本地消息表")
        void testSaveMessage_Success() {
            // Arrange
            when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> transactionalMessageService.saveMessage(testMessage));

            // Assert
            verify(jdbcTemplate).update(contains("INSERT INTO sys_message_log"),
                    eq("msg-001"),
                    eq("test-topic"),
                    eq("test.routing.key"),
                    eq("test-tag"),
                    anyString(), // payload JSON
                    anyString(), // headers JSON
                    any(LocalDateTime.class),
                    eq(0L),
                    eq(0),
                    eq(3),
                    eq("partition-001"),
                    eq("PENDING"),
                    any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("更新状态测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新消息状态")
        void testUpdateStatus() {
            // Arrange
            when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

            // Act
            transactionalMessageService.updateStatus("msg-001", MessageStatus.SENT);

            // Assert
            verify(jdbcTemplate).update(contains("UPDATE sys_message_log SET status"),
                    eq("SENT"), any(LocalDateTime.class), eq("msg-001"));
        }

        @Test
        @DisplayName("更新消息为已发送状态")
        void testUpdateSentStatus() {
            // Arrange
            when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);

            // Act
            transactionalMessageService.updateSentStatus("msg-001", "rocketmq-msg-001");

            // Assert
            verify(jdbcTemplate).update(contains("UPDATE sys_message_log SET status"),
                    eq("SENT"), eq("rocketmq-msg-001"), any(LocalDateTime.class), eq("msg-001"));
        }

        @Test
        @DisplayName("更新消息为失败状态")
        void testUpdateFailedStatus() {
            // Arrange
            when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);

            // Act
            transactionalMessageService.updateFailedStatus("msg-001", "Network error");

            // Assert
            verify(jdbcTemplate).update(contains("UPDATE sys_message_log SET status"),
                    eq("FAILED"), eq("Network error"), any(LocalDateTime.class), eq("msg-001"));
        }
    }

    @Nested
    @DisplayName("重试计数测试")
    class RetryCountTests {

        @Test
        @DisplayName("增加重试次数")
        void testIncrementRetryCount() {
            // Arrange
            when(jdbcTemplate.update(contains("retry_count = retry_count + 1"), any(LocalDateTime.class), anyString())).thenReturn(1);

            // Act
            transactionalMessageService.incrementRetryCount("msg-001");

            // Assert
            verify(jdbcTemplate).update(contains("retry_count = retry_count + 1"),
                    any(LocalDateTime.class), eq("msg-001"));
        }
    }

    @Nested
    @DisplayName("补偿任务测试")
    class CompensationTests {

        @Test
        @DisplayName("补偿超时消息 - 无超时消息")
        void testCompensateTimeoutMessages_NoMessages() {
            // Arrange
            List<Map<String, Object>> emptyList = new ArrayList<>();
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(emptyList);

            // Act
            assertDoesNotThrow(() -> transactionalMessageService.compensateTimeoutMessages());

            // Assert
            verify(jdbcTemplate).queryForList(anyString(),
                    eq("PENDING"), eq("FAILED"), eq(30));
        }

        @Test
        @DisplayName("补偿超时消息 - 有超时消息")
        void testCompensateTimeoutMessages_HasMessages() {
            // Arrange
            List<Map<String, Object>> timeoutMessages = new ArrayList<>();
            Map<String, Object> msg = new HashMap<>();
            msg.put("message_id", "msg-001");
            msg.put("topic", "test-topic");
            timeoutMessages.add(msg);
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(timeoutMessages);

            // Act
            assertDoesNotThrow(() -> transactionalMessageService.compensateTimeoutMessages());

            // Assert
            verify(jdbcTemplate).queryForList(anyString(),
                    eq("PENDING"), eq("FAILED"), eq(30));
        }

        @Test
        @DisplayName("补偿超时消息 - 异常处理")
        void testCompensateTimeoutMessages_Exception() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - 不应抛出异常
            assertDoesNotThrow(() -> transactionalMessageService.compensateTimeoutMessages());
        }
    }

    @Nested
    @DisplayName("清理过期消息测试")
    class CleanupTests {

        @Test
        @DisplayName("清理过期消息")
        void testCleanExpiredMessages() {
            // Arrange
            when(jdbcTemplate.update(anyString(), any(), any())).thenReturn(10);

            // Act
            assertDoesNotThrow(() -> transactionalMessageService.cleanExpiredMessages());

            // Assert
            verify(jdbcTemplate).update(contains("DELETE FROM sys_message_log"),
                    eq("CONSUMED"), eq("DEAD_LETTER"));
        }

        @Test
        @DisplayName("清理过期消息 - 异常处理")
        void testCleanExpiredMessages_Exception() {
            // Arrange
            when(jdbcTemplate.update(anyString(), any(), any()))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - 不应抛出异常
            assertDoesNotThrow(() -> transactionalMessageService.cleanExpiredMessages());
        }
    }
}
