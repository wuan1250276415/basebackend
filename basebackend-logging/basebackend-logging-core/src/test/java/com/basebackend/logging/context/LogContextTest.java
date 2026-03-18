package com.basebackend.logging.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogContext 日志上下文单元测试
 */
class LogContextTest {

    @AfterEach
    void tearDown() {
        LogContext.clear();
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("初始化后 traceId 和 requestId 不为空")
        void shouldSetTraceIdAndRequestId() {
            LogContext.init();
            assertThat(LogContext.getTraceId()).isNotBlank().hasSize(32);
            assertThat(LogContext.getRequestId()).isNotBlank().hasSize(32);
        }

        @Test
        @DisplayName("每次初始化生成不同的 traceId")
        void shouldGenerateDifferentTraceIds() {
            LogContext.init();
            String first = LogContext.getTraceId();
            LogContext.clear();
            LogContext.init();
            String second = LogContext.getTraceId();
            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("set/get 各字段")
    class SetGet {

        @Test
        @DisplayName("traceId 存取")
        void shouldSetAndGetTraceId() {
            LogContext.setTraceId("abc123");
            assertThat(LogContext.getTraceId()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("requestId 存取")
        void shouldSetAndGetRequestId() {
            LogContext.setRequestId("req-001");
            assertThat(LogContext.getRequestId()).isEqualTo("req-001");
        }

        @Test
        @DisplayName("userId 存取")
        void shouldSetAndGetUserId() {
            LogContext.setUserId("100");
            assertThat(LogContext.getUserId()).isEqualTo("100");
        }

        @Test
        @DisplayName("username 存取")
        void shouldSetAndGetUsername() {
            LogContext.setUsername("admin");
            assertThat(LogContext.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("ipAddress 存取")
        void shouldSetAndGetIpAddress() {
            LogContext.setIpAddress("198.51.100.1");
            assertThat(LogContext.getIpAddress()).isEqualTo("198.51.100.1");
        }

        @Test
        @DisplayName("uri 存取")
        void shouldSetAndGetUri() {
            LogContext.setUri("/api/users");
            assertThat(LogContext.getUri()).isEqualTo("/api/users");
        }

        @Test
        @DisplayName("method 存取")
        void shouldSetAndGetMethod() {
            LogContext.setMethod("POST");
            assertThat(LogContext.getMethod()).isEqualTo("POST");
        }
    }

    @Nested
    @DisplayName("自定义属性")
    class CustomProperties {

        @Test
        @DisplayName("put/get 自定义属性")
        void shouldSetAndGetCustom() {
            LogContext.put("tenantId", "tenant-001");
            assertThat(LogContext.get("tenantId")).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("不存在的属性返回 null")
        void shouldReturnNullForMissing() {
            assertThat(LogContext.get("nonExistent")).isNull();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("清除后所有字段为 null")
        void shouldClearAll() {
            LogContext.init();
            LogContext.setUserId("100");
            LogContext.setUsername("admin");
            LogContext.setIpAddress("127.0.0.1");
            LogContext.setUri("/api/test");
            LogContext.setMethod("GET");
            LogContext.put("custom", "value");

            LogContext.clear();

            assertThat(LogContext.getTraceId()).isNull();
            assertThat(LogContext.getRequestId()).isNull();
            assertThat(LogContext.getUserId()).isNull();
            assertThat(LogContext.getUsername()).isNull();
            assertThat(LogContext.getIpAddress()).isNull();
            assertThat(LogContext.getUri()).isNull();
            assertThat(LogContext.getMethod()).isNull();
            assertThat(LogContext.get("custom")).isNull();
        }
    }

    @Nested
    @DisplayName("generateTraceId / generateRequestId")
    class Generate {

        @Test
        @DisplayName("生成的 ID 为 32 位十六进制")
        void shouldGenerate32HexChars() {
            String traceId = LogContext.generateTraceId();
            assertThat(traceId).hasSize(32).matches("[0-9a-f]+");

            String requestId = LogContext.generateRequestId();
            assertThat(requestId).hasSize(32).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("每次生成不同的 ID")
        void shouldGenerateUniqueIds() {
            assertThat(LogContext.generateTraceId()).isNotEqualTo(LogContext.generateTraceId());
            assertThat(LogContext.generateRequestId()).isNotEqualTo(LogContext.generateRequestId());
        }
    }
}
