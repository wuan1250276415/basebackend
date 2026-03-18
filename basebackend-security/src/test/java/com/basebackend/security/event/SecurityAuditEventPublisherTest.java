package com.basebackend.security.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SecurityAuditEventPublisher 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityAuditEventPublisher 安全审计事件发布器测试")
class SecurityAuditEventPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SecurityAuditEventPublisher auditEventPublisher;

    @Test
    @DisplayName("应发布包含正确字段的安全审计事件")
    void shouldPublishEventWithCorrectFields() {
        auditEventPublisher.publish(this, SecurityEventType.AUTHENTICATION_SUCCESS,
                "testuser", "198.51.100.1", "认证成功");

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        SecurityAuditEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(SecurityEventType.AUTHENTICATION_SUCCESS);
        assertThat(event.getPrincipal()).isEqualTo("testuser");
        assertThat(event.getRemoteAddress()).isEqualTo("198.51.100.1");
        assertThat(event.getDetail()).isEqualTo("认证成功");
        assertThat(event.getEventTime()).isNotNull();
        assertThat(event.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("应支持带元数据的事件发布")
    void shouldPublishEventWithMetadata() {
        Map<String, Object> metadata = Map.of("path", "/api/auth/login", "attempts", 3);
        auditEventPublisher.publish(this, SecurityEventType.RATE_LIMIT_EXCEEDED,
                null, "10.0.0.1", "速率限制触发", metadata);

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        SecurityAuditEvent event = captor.getValue();
        assertThat(event.getMetadata()).containsEntry("path", "/api/auth/login");
        assertThat(event.getMetadata()).containsEntry("attempts", 3);
    }

    @Test
    @DisplayName("发布失败应降级为日志，不抛异常")
    void shouldNotThrowWhenPublishFails() {
        doThrow(new RuntimeException("event bus error")).when(eventPublisher).publishEvent(any());

        // 不应抛出异常
        auditEventPublisher.publish(this, SecurityEventType.AUTHENTICATION_FAILURE,
                "user", "1.2.3.4", "失败");
    }
}
