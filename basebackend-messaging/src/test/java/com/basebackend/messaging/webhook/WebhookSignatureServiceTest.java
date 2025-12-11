package com.basebackend.messaging.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebhookSignatureService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Webhook签名服务测试")
class WebhookSignatureServiceTest {

    private WebhookSignatureService signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new WebhookSignatureService();
    }

    @Nested
    @DisplayName("签名生成测试")
    class GenerateSignatureTests {

        @Test
        @DisplayName("生成签名 - 成功")
        void testGenerateSignature_Success() {
            // Arrange
            String payload = "{\"event\":\"test\",\"data\":\"hello\"}";
            String secret = "my-secret-key";
            long timestamp = 1700000000000L;

            // Act
            String signature = signatureService.generateSignature(payload, secret, timestamp);

            // Assert
            assertNotNull(signature);
            assertFalse(signature.isEmpty());
        }

        @Test
        @DisplayName("生成签名 - 相同输入产生相同签名")
        void testGenerateSignature_Deterministic() {
            // Arrange
            String payload = "{\"event\":\"test\"}";
            String secret = "secret123";
            long timestamp = 1700000000000L;

            // Act
            String signature1 = signatureService.generateSignature(payload, secret, timestamp);
            String signature2 = signatureService.generateSignature(payload, secret, timestamp);

            // Assert
            assertEquals(signature1, signature2);
        }

        @Test
        @DisplayName("生成签名 - 不同payload产生不同签名")
        void testGenerateSignature_DifferentPayload() {
            // Arrange
            String secret = "secret123";
            long timestamp = 1700000000000L;

            // Act
            String signature1 = signatureService.generateSignature("{\"a\":1}", secret, timestamp);
            String signature2 = signatureService.generateSignature("{\"a\":2}", secret, timestamp);

            // Assert
            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("生成签名 - 不同secret产生不同签名")
        void testGenerateSignature_DifferentSecret() {
            // Arrange
            String payload = "{\"event\":\"test\"}";
            long timestamp = 1700000000000L;

            // Act
            String signature1 = signatureService.generateSignature(payload, "secret1", timestamp);
            String signature2 = signatureService.generateSignature(payload, "secret2", timestamp);

            // Assert
            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("生成签名 - 不同timestamp产生不同签名")
        void testGenerateSignature_DifferentTimestamp() {
            // Arrange
            String payload = "{\"event\":\"test\"}";
            String secret = "secret123";

            // Act
            String signature1 = signatureService.generateSignature(payload, secret, 1700000000000L);
            String signature2 = signatureService.generateSignature(payload, secret, 1700000001000L);

            // Assert
            assertNotEquals(signature1, signature2);
        }
    }

    @Nested
    @DisplayName("签名验证测试")
    class VerifySignatureTests {

        @Test
        @DisplayName("验证签名 - 成功")
        void testVerifySignature_Success() {
            // Arrange
            String payload = "{\"event\":\"test\",\"data\":\"hello\"}";
            String secret = "my-secret-key";
            long timestamp = 1700000000000L;
            String signature = signatureService.generateSignature(payload, secret, timestamp);

            // Act
            boolean result = signatureService.verifySignature(payload, secret, timestamp, signature);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("验证签名 - 失败（签名不匹配）")
        void testVerifySignature_Failed_WrongSignature() {
            // Arrange
            String payload = "{\"event\":\"test\"}";
            String secret = "my-secret-key";
            long timestamp = 1700000000000L;

            // Act
            boolean result = signatureService.verifySignature(payload, secret, timestamp, "wrong-signature");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("验证签名 - 失败（payload被篡改）")
        void testVerifySignature_Failed_TamperedPayload() {
            // Arrange
            String originalPayload = "{\"event\":\"test\"}";
            String tamperedPayload = "{\"event\":\"hacked\"}";
            String secret = "my-secret-key";
            long timestamp = 1700000000000L;
            String signature = signatureService.generateSignature(originalPayload, secret, timestamp);

            // Act
            boolean result = signatureService.verifySignature(tamperedPayload, secret, timestamp, signature);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("验证签名 - 失败（secret不匹配）")
        void testVerifySignature_Failed_WrongSecret() {
            // Arrange
            String payload = "{\"event\":\"test\"}";
            long timestamp = 1700000000000L;
            String signature = signatureService.generateSignature(payload, "correct-secret", timestamp);

            // Act
            boolean result = signatureService.verifySignature(payload, "wrong-secret", timestamp, signature);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("添加签名头测试")
    class AddSignatureHeadersTests {

        @Test
        @DisplayName("添加签名头到HTTP请求")
        void testAddSignatureHeaders() {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            String payload = "{\"event\":\"test\"}";
            String secret = "my-secret-key";

            // Act
            signatureService.addSignatureHeaders(headers, payload, secret);

            // Assert
            assertTrue(headers.containsKey("X-Webhook-Signature"));
            assertTrue(headers.containsKey("X-Webhook-Timestamp"));
            assertNotNull(headers.getFirst("X-Webhook-Signature"));
            assertNotNull(headers.getFirst("X-Webhook-Timestamp"));
        }

        @Test
        @DisplayName("添加的签名可以被验证")
        void testAddSignatureHeaders_Verifiable() {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            String payload = "{\"event\":\"test\"}";
            String secret = "my-secret-key";

            // Act
            signatureService.addSignatureHeaders(headers, payload, secret);

            // Assert
            String signature = headers.getFirst("X-Webhook-Signature");
            long timestamp = Long.parseLong(headers.getFirst("X-Webhook-Timestamp"));
            assertTrue(signatureService.verifySignature(payload, secret, timestamp, signature));
        }
    }
}
