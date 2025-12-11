package com.basebackend.messaging.webhook;

import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebhookInvoker 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Webhook调用服务测试")
class WebhookInvokerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WebhookSignatureService signatureService;

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private WebhookInvoker webhookInvoker;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    private WebhookConfig webhookConfig;
    private WebhookEvent webhookEvent;

    @BeforeEach
    void setUp() {
        webhookConfig = new WebhookConfig();
        webhookConfig.setId(1L);
        webhookConfig.setUrl("https://example.com/webhook");
        webhookConfig.setMethod("POST");
        webhookConfig.setMaxRetries(3);
        webhookConfig.setRetryInterval(60);
        webhookConfig.setSignatureEnabled(false);

        webhookEvent = new WebhookEvent();
        webhookEvent.setEventId("event-001");
        webhookEvent.setEventType("user.created");
        webhookEvent.setData(Map.of("userId", 123, "username", "testuser"));
        webhookEvent.setTimestamp(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Webhook调用测试")
    class InvokeTests {

        @Test
        @DisplayName("POST请求调用成功")
        void testInvoke_PostSuccess() {
            // Arrange
            ResponseEntity<String> response = new ResponseEntity<>("{\"status\":\"ok\"}", HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(response);

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertNotNull(log);
            assertTrue(log.getSuccess());
            assertEquals(200, log.getResponseStatus());
            assertEquals("{\"status\":\"ok\"}", log.getResponseBody());
            assertEquals("event-001", log.getEventId());
            assertEquals("user.created", log.getEventType());
        }

        @Test
        @DisplayName("PUT请求调用成功")
        void testInvoke_PutSuccess() {
            // Arrange
            webhookConfig.setMethod("PUT");
            ResponseEntity<String> response = new ResponseEntity<>("{\"updated\":true}", HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(response);

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertNotNull(log);
            assertTrue(log.getSuccess());
            assertEquals(200, log.getResponseStatus());
        }

        @Test
        @DisplayName("调用失败 - HTTP错误状态码")
        void testInvoke_HttpError() {
            // Arrange
            ResponseEntity<String> response = new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(response);

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertNotNull(log);
            assertFalse(log.getSuccess());
            assertEquals(500, log.getResponseStatus());
            assertTrue(log.getErrorMessage().contains("HTTP 500"));
        }

        @Test
        @DisplayName("调用失败 - 网络异常并触发重试")
        void testInvoke_NetworkException_ScheduleRetry() {
            // Arrange
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RestClientException("Connection refused"));
            when(messageProducer.sendDelay(any(Message.class), anyLong())).thenReturn("retry-msg-001");

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertNotNull(log);
            assertFalse(log.getSuccess());
            assertEquals("Connection refused", log.getErrorMessage());
            verify(messageProducer).sendDelay(any(Message.class), anyLong());
        }

        @Test
        @DisplayName("调用失败 - 不支持的HTTP方法")
        void testInvoke_UnsupportedMethod() {
            // Arrange
            webhookConfig.setMethod("DELETE");

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertNotNull(log);
            assertFalse(log.getSuccess());
            assertTrue(log.getErrorMessage().contains("Unsupported HTTP method"));
        }
    }

    @Nested
    @DisplayName("签名测试")
    class SignatureTests {

        @Test
        @DisplayName("启用签名时添加签名头")
        void testInvoke_WithSignature() {
            // Arrange
            webhookConfig.setSignatureEnabled(true);
            webhookConfig.setSecret("webhook-secret");
            ResponseEntity<String> response = new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(response);
            doNothing().when(signatureService).addSignatureHeaders(any(HttpHeaders.class), anyString(), anyString());

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertTrue(log.getSuccess());
            verify(signatureService).addSignatureHeaders(any(HttpHeaders.class), anyString(), eq("webhook-secret"));
        }
    }

    @Nested
    @DisplayName("自定义请求头测试")
    class CustomHeadersTests {

        @Test
        @DisplayName("添加自定义请求头")
        void testInvoke_WithCustomHeaders() {
            // Arrange
            webhookConfig.setHeaders("{\"X-Custom-Header\":\"custom-value\",\"Authorization\":\"Bearer token\"}");
            ResponseEntity<String> response = new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(response);

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertTrue(log.getSuccess());
            assertTrue(log.getRequestHeaders().contains("X-Custom-Header"));
        }
    }

    @Nested
    @DisplayName("异步调用测试")
    class AsyncInvokeTests {

        @Test
        @DisplayName("异步调用Webhook")
        void testInvokeAsync() {
            // Arrange
            when(messageProducer.send(any(Message.class))).thenReturn("async-msg-001");

            // Act
            webhookInvoker.invokeAsync(webhookConfig, webhookEvent);

            // Assert
            verify(messageProducer).send(messageCaptor.capture());
            Message<?> capturedMessage = messageCaptor.getValue();
            assertEquals("webhook.invoke", capturedMessage.getTopic());
            assertTrue(capturedMessage.getRoutingKey().contains("webhook.invoke.1"));
        }
    }

    @Nested
    @DisplayName("重试机制测试")
    class RetryTests {

        @Test
        @DisplayName("重试延迟使用指数退避")
        void testRetry_ExponentialBackoff() {
            // Arrange
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RestClientException("Timeout"));
            when(messageProducer.sendDelay(any(Message.class), anyLong())).thenReturn("retry-msg");

            // Act
            webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            verify(messageProducer).sendDelay(messageCaptor.capture(), eq(60000L)); // 第一次重试：60秒
        }

        @Test
        @DisplayName("超过最大重试次数不再重试")
        void testRetry_MaxRetriesExceeded() {
            // Arrange
            webhookConfig.setMaxRetries(0); // 不允许重试
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RestClientException("Timeout"));

            // Act
            WebhookLog log = webhookInvoker.invoke(webhookConfig, webhookEvent);

            // Assert
            assertFalse(log.getSuccess());
            verify(messageProducer, never()).sendDelay(any(Message.class), anyLong());
        }
    }
}
