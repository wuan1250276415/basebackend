package com.basebackend.security.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全异常类单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("Security Exception 单元测试")
class SecurityExceptionTest {

    @Nested
    @DisplayName("SecurityException 基类测试")
    class SecurityExceptionBaseTests {

        @Test
        @DisplayName("创建带消息的异常")
        void createWithMessage_shouldHaveMessage() {
            // When
            SecurityException ex = new SecurityException("Test error");

            // Then
            assertTrue(ex.getMessage().contains("Test error"));
            assertEquals(SecurityException.ErrorCode.UNKNOWN, ex.getErrorCode());
        }

        @Test
        @DisplayName("创建带错误码的异常")
        void createWithErrorCode_shouldHaveErrorCode() {
            // When
            SecurityException ex = new SecurityException("Config error",
                    SecurityException.ErrorCode.CONFIGURATION_ERROR);

            // Then
            assertEquals(SecurityException.ErrorCode.CONFIGURATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("SEC_0001"));
        }

        @Test
        @DisplayName("创建带详细信息的异常")
        void createWithDetails_shouldHaveDetails() {
            // When
            SecurityException ex = new SecurityException(
                    "Error occurred",
                    SecurityException.ErrorCode.AUTHENTICATION_FAILED,
                    "Additional details");

            // Then
            assertEquals("Additional details", ex.getDetails());
            assertTrue(ex.getMessage().contains("details=Additional details"));
        }

        @Test
        @DisplayName("创建带原因的异常")
        void createWithCause_shouldHaveCause() {
            // Given
            Throwable cause = new RuntimeException("Root cause");

            // When
            SecurityException ex = new SecurityException("Wrapper error", cause);

            // Then
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("MTLSException 测试")
    class MTLSExceptionTests {

        @Test
        @DisplayName("SSL初始化失败异常")
        void sslInitFailed_shouldCreateCorrectException() {
            // When
            MTLSException ex = MTLSException.sslInitFailed("Certificate load error",
                    new RuntimeException("IO error"));

            // Then
            assertEquals(SecurityException.ErrorCode.SSL_INIT_FAILED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("SSL上下文初始化失败"));
            assertNotNull(ex.getCause());
        }

        @Test
        @DisplayName("证书无效异常")
        void certificateInvalid_shouldCreateCorrectException() {
            // When
            MTLSException ex = MTLSException.certificateInvalid("/path/to/cert.pem");

            // Then
            assertEquals(SecurityException.ErrorCode.CERTIFICATE_INVALID, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("证书无效"));
        }

        @Test
        @DisplayName("证书过期异常")
        void certificateExpired_shouldCreateCorrectException() {
            // When
            MTLSException ex = MTLSException.certificateExpired("/path/to/cert.pem");

            // Then
            assertEquals(SecurityException.ErrorCode.CERTIFICATE_EXPIRED, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("证书已过期"));
        }

        @Test
        @DisplayName("证书未找到异常")
        void certificateNotFound_shouldCreateCorrectException() {
            // When
            MTLSException ex = MTLSException.certificateNotFound("/path/to/cert.pem");

            // Then
            assertEquals(SecurityException.ErrorCode.CERTIFICATE_NOT_FOUND, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("证书未找到"));
        }

        @Test
        @DisplayName("密钥库错误异常")
        void keystoreError_shouldCreateCorrectException() {
            // When
            MTLSException ex = MTLSException.keystoreError("/path/to/keystore",
                    new RuntimeException("Load error"));

            // Then
            assertEquals(SecurityException.ErrorCode.KEYSTORE_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("密钥库加载失败"));
            assertNotNull(ex.getCause());
        }
    }

    @Nested
    @DisplayName("ZeroTrustException 测试")
    class ZeroTrustExceptionTests {

        @Test
        @DisplayName("高风险检测异常")
        void highRiskDetected_shouldCreateCorrectException() {
            // When
            ZeroTrustException ex = ZeroTrustException.highRiskDetected("user123", 85);

            // Then
            assertEquals(SecurityException.ErrorCode.HIGH_RISK_DETECTED, ex.getErrorCode());
            assertEquals("user123", ex.getUserId());
            assertEquals(Integer.valueOf(85), ex.getRiskScore());
            assertTrue(ex.getMessage().contains("检测到高风险"));
        }

        @Test
        @DisplayName("设备不受信任异常")
        void deviceNotTrusted_shouldCreateCorrectException() {
            // When
            ZeroTrustException ex = ZeroTrustException.deviceNotTrusted("user123", "device-456");

            // Then
            assertEquals(SecurityException.ErrorCode.DEVICE_NOT_TRUSTED, ex.getErrorCode());
            assertEquals("user123", ex.getUserId());
            assertTrue(ex.getMessage().contains("设备不受信任"));
        }

        @Test
        @DisplayName("违反策略异常")
        void policyViolation_shouldCreateCorrectException() {
            // When
            ZeroTrustException ex = ZeroTrustException.policyViolation("user123", "AccessPolicy");

            // Then
            assertEquals(SecurityException.ErrorCode.POLICY_VIOLATION, ex.getErrorCode());
            assertEquals("user123", ex.getUserId());
            assertTrue(ex.getMessage().contains("违反安全策略"));
        }

        @Test
        @DisplayName("风险评估失败异常")
        void riskAssessmentFailed_shouldCreateCorrectException() {
            // When
            ZeroTrustException ex = ZeroTrustException.riskAssessmentFailed("user123",
                    new RuntimeException("Assessment error"));

            // Then
            assertEquals(SecurityException.ErrorCode.RISK_ASSESSMENT_FAILED, ex.getErrorCode());
            assertEquals("user123", ex.getUserId());
            assertNotNull(ex.getCause());
        }

        @Test
        @DisplayName("异常消息应包含userId")
        void exceptionMessage_shouldContainUserId() {
            // When
            ZeroTrustException ex = ZeroTrustException.highRiskDetected("testUser", 90);

            // Then
            assertTrue(ex.getMessage().contains("userId=testUser"));
        }

        @Test
        @DisplayName("异常消息应包含riskScore")
        void exceptionMessage_shouldContainRiskScore() {
            // When
            ZeroTrustException ex = ZeroTrustException.highRiskDetected("testUser", 95);

            // Then
            assertTrue(ex.getMessage().contains("riskScore=95"));
        }
    }

    @Nested
    @DisplayName("ErrorCode 测试")
    class ErrorCodeTests {

        @Test
        @DisplayName("错误码应有唯一的代码")
        void errorCodes_shouldHaveUniqueCodes() {
            // Given
            SecurityException.ErrorCode[] codes = SecurityException.ErrorCode.values();

            // When & Then
            for (int i = 0; i < codes.length; i++) {
                for (int j = i + 1; j < codes.length; j++) {
                    assertNotEquals(codes[i].getCode(), codes[j].getCode(),
                            "错误码应唯一: " + codes[i] + " vs " + codes[j]);
                }
            }
        }

        @Test
        @DisplayName("错误码应有描述")
        void errorCodes_shouldHaveDescription() {
            // When & Then
            for (SecurityException.ErrorCode code : SecurityException.ErrorCode.values()) {
                assertNotNull(code.getDescription());
                assertFalse(code.getDescription().isEmpty());
            }
        }

        @Test
        @DisplayName("认证相关错误码应以SEC_1开头")
        void authenticationErrors_shouldStartWithSEC1() {
            // Then
            assertTrue(SecurityException.ErrorCode.AUTHENTICATION_FAILED.getCode().startsWith("SEC_1"));
            assertTrue(SecurityException.ErrorCode.TOKEN_INVALID.getCode().startsWith("SEC_1"));
            assertTrue(SecurityException.ErrorCode.TOKEN_EXPIRED.getCode().startsWith("SEC_1"));
        }

        @Test
        @DisplayName("授权相关错误码应以SEC_2开头")
        void authorizationErrors_shouldStartWithSEC2() {
            // Then
            assertTrue(SecurityException.ErrorCode.ACCESS_DENIED.getCode().startsWith("SEC_2"));
            assertTrue(SecurityException.ErrorCode.PERMISSION_DENIED.getCode().startsWith("SEC_2"));
        }

        @Test
        @DisplayName("零信任相关错误码应以SEC_3开头")
        void zeroTrustErrors_shouldStartWithSEC3() {
            // Then
            assertTrue(SecurityException.ErrorCode.RISK_ASSESSMENT_FAILED.getCode().startsWith("SEC_3"));
            assertTrue(SecurityException.ErrorCode.HIGH_RISK_DETECTED.getCode().startsWith("SEC_3"));
        }

        @Test
        @DisplayName("mTLS相关错误码应以SEC_4开头")
        void mtlsErrors_shouldStartWithSEC4() {
            // Then
            assertTrue(SecurityException.ErrorCode.SSL_INIT_FAILED.getCode().startsWith("SEC_4"));
            assertTrue(SecurityException.ErrorCode.CERTIFICATE_INVALID.getCode().startsWith("SEC_4"));
        }
    }
}
