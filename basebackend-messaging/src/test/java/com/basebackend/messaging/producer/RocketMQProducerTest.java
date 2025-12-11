package com.basebackend.messaging.producer;

import com.basebackend.messaging.config.MessagingProperties;
import com.basebackend.messaging.exception.MessageSendException;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.transaction.TransactionalMessageService;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RocketMQProducer 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RocketMQ消息生产者测试")
class RocketMQProducerTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private TransactionalMessageService transactionalMessageService;

    @InjectMocks
    private RocketMQProducer rocketMQProducer;

    private Message<String> testMessage;
    private SendResult successSendResult;

    @BeforeEach
    void setUp() {
        // 初始化测试消息
        testMessage = Message.<String>builder()
                .messageId("msg-001")
                .topic("test-topic")
                .tags("test-tag")
                .messageType("TEST")
                .payload("test payload")
                .sendTime(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        // 初始化成功的发送结果
        successSendResult = new SendResult();
        successSendResult.setSendStatus(SendStatus.SEND_OK);
        successSendResult.setMsgId("rocketmq-msg-001");
        MessageQueue messageQueue = new MessageQueue("test-topic", "broker-a", 0);
        successSendResult.setMessageQueue(messageQueue);
    }

    @Nested
    @DisplayName("普通消息发送测试")
    class SendTests {

        @Test
        @DisplayName("发送消息成功")
        void testSend_Success() {
            // Arrange
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class)))
                    .thenReturn(successSendResult);

            // Act
            String msgId = rocketMQProducer.send(testMessage);

            // Assert
            assertNotNull(msgId);
            assertEquals("rocketmq-msg-001", msgId);
            verify(rocketMQTemplate).syncSend(eq("test-topic:test-tag"), any(org.springframework.messaging.Message.class));
        }

        @Test
        @DisplayName("发送消息成功 - 无Tag使用默认Topic")
        void testSend_WithoutTag() {
            // Arrange
            testMessage.setTags(null);
            testMessage.setTopic(null);
            MessagingProperties.RocketMQ rocketMQProps = new MessagingProperties.RocketMQ();
            rocketMQProps.setDefaultTopic("default-topic");
            lenient().when(messagingProperties.getRocketmq()).thenReturn(rocketMQProps);
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class)))
                    .thenReturn(successSendResult);

            // Act
            String msgId = rocketMQProducer.send(testMessage);

            // Assert
            assertNotNull(msgId);
            verify(rocketMQTemplate).syncSend(eq("default-topic"), any(org.springframework.messaging.Message.class));
        }

        @Test
        @DisplayName("发送消息失败 - 发送状态非OK")
        void testSend_FailedStatus() {
            // Arrange
            SendResult failedResult = new SendResult();
            failedResult.setSendStatus(SendStatus.FLUSH_DISK_TIMEOUT);
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class)))
                    .thenReturn(failedResult);

            // Act & Assert
            MessageSendException exception = assertThrows(MessageSendException.class,
                    () -> rocketMQProducer.send(testMessage));
            assertTrue(exception.getMessage().contains("消息发送失败"));
        }

        @Test
        @DisplayName("发送消息失败 - 异常")
        void testSend_Exception() {
            // Arrange
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class)))
                    .thenThrow(new RuntimeException("Network error"));

            // Act & Assert
            MessageSendException exception = assertThrows(MessageSendException.class,
                    () -> rocketMQProducer.send(testMessage));
            assertTrue(exception.getMessage().contains("消息发送失败"));
        }
    }

    @Nested
    @DisplayName("延迟消息发送测试")
    class SendDelayTests {

        @Test
        @DisplayName("发送延迟消息成功")
        void testSendDelay_Success() {
            // Arrange
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class), anyLong(), anyInt()))
                    .thenReturn(successSendResult);

            // Act
            String msgId = rocketMQProducer.sendDelay(testMessage, 60000L);

            // Assert
            assertNotNull(msgId);
            assertEquals("rocketmq-msg-001", msgId);
            verify(rocketMQTemplate).syncSend(eq("test-topic:test-tag"), 
                    any(org.springframework.messaging.Message.class), eq(3000L), anyInt());
        }

        @Test
        @DisplayName("发送延迟消息失败")
        void testSendDelay_Failed() {
            // Arrange
            SendResult failedResult = new SendResult();
            failedResult.setSendStatus(SendStatus.SLAVE_NOT_AVAILABLE);
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class), anyLong(), anyInt()))
                    .thenReturn(failedResult);

            // Act & Assert
            MessageSendException exception = assertThrows(MessageSendException.class,
                    () -> rocketMQProducer.sendDelay(testMessage, 60000L));
            assertTrue(exception.getMessage().contains("延迟消息发送失败"));
        }
    }

    @Nested
    @DisplayName("事务消息发送测试")
    class SendTransactionalTests {

        @Test
        @DisplayName("发送事务消息成功")
        void testSendTransactional_Success() {
            // Arrange
            doNothing().when(transactionalMessageService).saveMessage(any(Message.class));
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class)))
                    .thenReturn(successSendResult);
            doNothing().when(transactionalMessageService).updateSentStatus(anyString(), anyString());

            // Act
            String msgId = rocketMQProducer.sendTransactional(testMessage);

            // Assert
            assertNotNull(msgId);
            assertEquals("rocketmq-msg-001", msgId);
            verify(transactionalMessageService).saveMessage(testMessage);
            verify(transactionalMessageService).updateSentStatus("msg-001", "rocketmq-msg-001");
        }

        @Test
        @DisplayName("发送事务消息失败 - 发送异常")
        void testSendTransactional_SendFailed() {
            // Arrange
            doNothing().when(transactionalMessageService).saveMessage(any(Message.class));
            when(rocketMQTemplate.syncSend(anyString(), any(org.springframework.messaging.Message.class)))
                    .thenThrow(new RuntimeException("Send failed"));
            doNothing().when(transactionalMessageService).updateFailedStatus(anyString(), anyString());

            // Act & Assert
            MessageSendException exception = assertThrows(MessageSendException.class,
                    () -> rocketMQProducer.sendTransactional(testMessage));
            assertTrue(exception.getMessage().contains("事务消息发送失败"));
            verify(transactionalMessageService).updateFailedStatus(eq("msg-001"), anyString());
        }
    }

    @Nested
    @DisplayName("顺序消息发送测试")
    class SendOrderedTests {

        @Test
        @DisplayName("发送顺序消息成功")
        void testSendOrdered_Success() {
            // Arrange
            when(rocketMQTemplate.syncSendOrderly(anyString(), any(org.springframework.messaging.Message.class), anyString()))
                    .thenReturn(successSendResult);

            // Act
            String msgId = rocketMQProducer.sendOrdered(testMessage, "order-001");

            // Assert
            assertNotNull(msgId);
            assertEquals("rocketmq-msg-001", msgId);
            verify(rocketMQTemplate).syncSendOrderly(eq("test-topic:test-tag"), 
                    any(org.springframework.messaging.Message.class), eq("order-001"));
        }

        @Test
        @DisplayName("发送顺序消息失败")
        void testSendOrdered_Failed() {
            // Arrange
            SendResult failedResult = new SendResult();
            failedResult.setSendStatus(SendStatus.FLUSH_SLAVE_TIMEOUT);
            when(rocketMQTemplate.syncSendOrderly(anyString(), any(org.springframework.messaging.Message.class), anyString()))
                    .thenReturn(failedResult);

            // Act & Assert
            MessageSendException exception = assertThrows(MessageSendException.class,
                    () -> rocketMQProducer.sendOrdered(testMessage, "order-001"));
            assertTrue(exception.getMessage().contains("顺序消息发送失败"));
        }
    }
}
