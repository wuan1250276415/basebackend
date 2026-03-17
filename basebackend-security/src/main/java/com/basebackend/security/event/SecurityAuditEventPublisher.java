package com.basebackend.security.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 安全审计事件发布器
 * <p>
 * 封装 {@link ApplicationEventPublisher}，提供便捷的事件发布方法，
 * 并在发布失败时降级为日志输出，避免审计功能异常影响主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布安全审计事件
     */
    public void publish(Object source,
                        SecurityEventType eventType,
                        String principal,
                        String remoteAddress,
                        String detail) {
        publish(source, eventType, principal, remoteAddress, detail, null);
    }

    /**
     * 发布安全审计事件（带元数据）
     */
    public void publish(Object source,
                        SecurityEventType eventType,
                        String principal,
                        String remoteAddress,
                        String detail,
                        Map<String, Object> metadata) {
        try {
            SecurityAuditEvent event = new SecurityAuditEvent(
                    source, eventType, principal, remoteAddress, detail, metadata);
            eventPublisher.publishEvent(event);
            log.debug("安全审计事件已发布: type={}, principal={}, detail={}", eventType, principal, detail);
        } catch (Exception e) {
            // 审计事件发布失败不应影响主业务流程，降级为日志
            log.error("安全审计事件发布失败: type={}, principal={}, detail={}, error={}",
                    eventType, principal, detail, e.getMessage());
        }
    }
}
