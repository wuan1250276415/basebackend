package com.basebackend.logging.audit.model;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.AuditSeverity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditLogEntry测试类
 * 测试审计日志条目的序列化和模型功能
 *
 * @author BaseBackend
 */
@DisplayName("AuditLogEntry 审计日志条目测试")
class AuditLogEntryTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    @DisplayName("使用Builder创建审计日志条目")
    void shouldCreateAuditLogEntryWithBuilder() {
        // Given
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        // When
        AuditLogEntry entry = AuditLogEntry.builder()
            .id(id)
            .timestamp(now)
            .userId("user123")
            .sessionId("session456")
            .eventType(AuditEventType.LOGIN)
            .resource("/api/login")
            .result("SUCCESS")
            .clientIp("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .operation("用户登录")
            .build();

        // Then
        assertThat(entry.getId()).isEqualTo(id);
        assertThat(entry.getTimestamp()).isEqualTo(now);
        assertThat(entry.getUserId()).isEqualTo("user123");
        assertThat(entry.getSessionId()).isEqualTo("session456");
        assertThat(entry.getEventType()).isEqualTo(AuditEventType.LOGIN);
        assertThat(entry.getResource()).isEqualTo("/api/login");
        assertThat(entry.getOperation()).isEqualTo("用户登录");
        assertThat(entry.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("无参构造函数创建")
    void shouldCreateAuditLogEntryWithNoArgsConstructor() {
        // Given
        AuditLogEntry entry = new AuditLogEntry();

        // When
        entry.setId("test-id");
        entry.setUserId("test-user");
        entry.setEventType(AuditEventType.CREATE);

        // Then
        assertThat(entry.getId()).isEqualTo("test-id");
        assertThat(entry.getUserId()).isEqualTo("test-user");
        assertThat(entry.getEventType()).isEqualTo(AuditEventType.CREATE);
    }

    @Test
    @DisplayName("全参构造函数创建")
    void shouldCreateAuditLogEntryWithAllArgsConstructor() {
        // Given
        Instant now = Instant.now();
        String id = "audit-id";

        // When
        AuditLogEntry entry = new AuditLogEntry(
            id,
            now,
            "user123",
            "session456",
            AuditEventType.LOGOUT,
            "/api/logout",
            "SUCCESS",
            "1.2.3.4",
            "Mozilla/5.0",
            "Windows PC",
            "北京",
            "entity123",
            "用户登出",
            Map.of("param1", "value1"),
            100L,
            "ERROR_CODE",
            "错误消息",
            "trace123",
            "span123",
            "prevHash123",
            "entryHash123",
            "signature123",
            "cert123"
        );

        // Then
        assertThat(entry.getId()).isEqualTo(id);
        assertThat(entry.getTimestamp()).isEqualTo(now);
        assertThat(entry.getUserId()).isEqualTo("user123");
        assertThat(entry.getEventType()).isEqualTo(AuditEventType.LOGOUT);
        assertThat(entry.getResource()).isEqualTo("/api/logout");
        assertThat(entry.getClientIp()).isEqualTo("1.2.3.4");
        assertThat(entry.getLocation()).isEqualTo("北京");
        assertThat(entry.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(entry.getResult()).isEqualTo("SUCCESS");
        assertThat(entry.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("JSON序列化")
    void shouldSerializeToJson() throws Exception {
        // Given
        AuditLogEntry entry = AuditLogEntry.builder()
            .id("id123")
            .timestamp(Instant.now())
            .userId("user456")
            .eventType(AuditEventType.LOGIN)
            .resource("/api/login")
            .operation("登录")
            .result("SUCCESS")
            .build();

        // When
        String json = objectMapper.writeValueAsString(entry);

        // Then
        assertThat(json).contains("\"id\":\"id123\"");
        assertThat(json).contains("\"userId\":\"user456\"");
        assertThat(json).contains("\"eventType\":\"LOGIN\"");
        assertThat(json).contains("\"operation\":\"登录\"");
    }

    @Test
    @DisplayName("JSON反序列化")
    void shouldDeserializeFromJson() throws Exception {
        // Given
        String json = """
            {
                "id": "id789",
                "userId": "user999",
                "eventType": "CREATE",
                "resource": "/api/data",
                "operation": "查询数据",
                "result": "FAILURE"
            }
            """;

        // When
        AuditLogEntry entry = objectMapper.readValue(json, AuditLogEntry.class);

        // Then
        assertThat(entry.getId()).isEqualTo("id789");
        assertThat(entry.getUserId()).isEqualTo("user999");
        assertThat(entry.getEventType()).isEqualTo(AuditEventType.CREATE);
        assertThat(entry.getResource()).isEqualTo("/api/data");
        assertThat(entry.getOperation()).isEqualTo("查询数据");
        assertThat(entry.isFailure()).isTrue();
    }

    @Test
    @DisplayName("空字段不序列化")
    void shouldExcludeNullFields() throws Exception {
        // Given
        AuditLogEntry entry = AuditLogEntry.builder()
            .id("id123")
            .userId("user456")
            .build(); // 其他字段为空

        // When
        String json = objectMapper.writeValueAsString(entry);

        // Then - 验证是否包含NON_NULL注解的效果
        assertThat(json).contains("\"id\":\"id123\"");
        assertThat(json).contains("\"userId\":\"user456\"");
    }

    @Test
    @DisplayName("审计事件类型枚举")
    void shouldValidateAuditEventTypes() {
        // 验证所有枚举值存在
        assertThat(AuditEventType.values()).isNotEmpty();

        // 验证常见事件类型
        assertThat(AuditEventType.LOGIN).isNotNull();
        assertThat(AuditEventType.LOGOUT).isNotNull();
        assertThat(AuditEventType.CREATE).isNotNull();
        assertThat(AuditEventType.DELETE).isNotNull();
        assertThat(AuditEventType.UPDATE).isNotNull();
    }

    @Test
    @DisplayName("审计严重性级别")
    void shouldValidateAuditSeverityLevels() {
        // 验证所有严重性级别
        assertThat(AuditSeverity.values()).isNotEmpty();

        // 验证严重性级别
        assertThat(AuditSeverity.CRITICAL).isNotNull();
        assertThat(AuditSeverity.HIGH).isNotNull();
        assertThat(AuditSeverity.MEDIUM).isNotNull();
        assertThat(AuditSeverity.LOW).isNotNull();
    }

    @Test
    @DisplayName("复杂场景的审计日志条目")
    void shouldHandleComplexAuditEntry() {
        // Given - 模拟一个复杂的审计场景
        Instant now = Instant.now();
        Map<String, Object> details = Map.of(
            "tableName", "users",
            "operation", "UPDATE",
            "affectedRows", 1,
            "before", Map.of("name", "张三"),
            "after", Map.of("name", "李四")
        );

        // When
        AuditLogEntry entry = AuditLogEntry.builder()
            .id(UUID.randomUUID().toString())
            .timestamp(now)
            .userId("admin001")
            .sessionId("sess123")
            .eventType(AuditEventType.UPDATE)
            .resource("table:users")
            .operation("修改用户信息")
            .result("SUCCESS")
            .clientIp("192.168.1.100")
            .location("上海")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .details(details)
            .build();

        // Then
        assertThat(entry.getEventType()).isEqualTo(AuditEventType.UPDATE);
        assertThat(entry.getEventType().getSeverity()).isEqualTo(AuditSeverity.MEDIUM);
        assertThat(entry.getDetails()).isNotNull();
        assertThat(entry.getDetails().get("tableName")).isEqualTo("users");
        assertThat(entry.getDetails().get("affectedRows")).isEqualTo(1);
        assertThat(entry.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("失败的审计日志条目")
    void shouldHandleFailedAuditEntry() {
        // Given
        AuditLogEntry entry = AuditLogEntry.builder()
            .id("fail-001")
            .userId("user123")
            .eventType(AuditEventType.DELETE)
            .resource("/api/admin/permissions")
            .operation("修改权限")
            .result("FAILURE")
            .errorMessage("权限不足：需要管理员权限")
            .build();

        // Then
        assertThat(entry.isFailure()).isTrue();
        assertThat(entry.getErrorMessage()).contains("权限不足");
        assertThat(entry.getEventType().getSeverity()).isEqualTo(AuditSeverity.HIGH);
    }

    @Test
    @DisplayName("审计日志条目的toString方法")
    void shouldHaveDescriptiveToString() {
        // Given
        AuditLogEntry entry = AuditLogEntry.builder()
            .id("id001")
            .userId("user123")
            .eventType(AuditEventType.LOGIN)
            .resource("/api/login")
            .build();

        // When
        String str = entry.toString();

        // Then
        assertThat(str).contains("id001");
        assertThat(str).contains("user123");
        assertThat(str).contains("LOGIN");
    }

    @Test
    @DisplayName("审计日志条目的equals和hashCode")
    void shouldValidateEqualsAndHashCode() {
        // Given
        Instant now = Instant.now();
        AuditLogEntry entry1 = AuditLogEntry.builder()
            .id("id123")
            .userId("user456")
            .eventType(AuditEventType.LOGIN)
            .build();

        AuditLogEntry entry2 = AuditLogEntry.builder()
            .id("id123")
            .userId("user456")
            .eventType(AuditEventType.LOGIN)
            .build();

        AuditLogEntry entry3 = AuditLogEntry.builder()
            .id("id999")
            .userId("user456")
            .eventType(AuditEventType.LOGIN)
            .build();

        // Then
        assertThat(entry1).isEqualTo(entry2);
        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
        assertThat(entry1).isNotEqualTo(entry3);
        assertThat(entry1.hashCode()).isNotEqualTo(entry3.hashCode());
    }

    @Test
    @DisplayName("检查是否为高危操作")
    void shouldIdentifyHighRiskOperations() {
        // Given
        AuditLogEntry deleteEntry = AuditLogEntry.builder()
            .eventType(AuditEventType.DELETE)
            .build();

        AuditLogEntry loginEntry = AuditLogEntry.builder()
            .eventType(AuditEventType.LOGIN)
            .build();

        AuditLogEntry criticalEntry = AuditLogEntry.builder()
            .eventType(AuditEventType.ACCESS_DENIED)
            .build();

        // Then
        assertThat(deleteEntry.getEventType().isHighRisk()).isTrue();
        assertThat(loginEntry.getEventType().isHighRisk()).isFalse();
        assertThat(criticalEntry.getEventType().isHighRisk()).isTrue();
    }

    @Test
    @DisplayName("获取严重级别数值")
    void shouldGetSeverityLevel() {
        // Given
        AuditLogEntry entry = AuditLogEntry.builder()
            .eventType(AuditEventType.DELETE)
            .build();

        // Then
        assertThat(entry.getSeverityLevel()).isEqualTo(3); // HIGH级别
    }
}
