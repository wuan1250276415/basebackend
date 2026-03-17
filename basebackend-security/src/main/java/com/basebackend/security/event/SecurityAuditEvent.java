package com.basebackend.security.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * 安全审计事件基类
 * <p>
 * 通过 Spring {@link org.springframework.context.ApplicationEventPublisher} 发布，
 * 下游可通过 {@code @EventListener} 或 {@code ApplicationListener} 订阅，
 * 与可观测性模块（日志、Metrics、告警）集成。
 */
@Getter
public class SecurityAuditEvent extends ApplicationEvent {

    private final SecurityEventType eventType;
    private final String principal;
    private final String remoteAddress;
    private final String detail;
    private final Map<String, Object> metadata;
    private final Instant eventTime;

    public SecurityAuditEvent(Object source,
                               SecurityEventType eventType,
                               String principal,
                               String remoteAddress,
                               String detail,
                               Map<String, Object> metadata) {
        super(source);
        this.eventType = eventType;
        this.principal = principal;
        this.remoteAddress = remoteAddress;
        this.detail = detail;
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
        this.eventTime = Instant.now();
    }

    public SecurityAuditEvent(Object source,
                               SecurityEventType eventType,
                               String principal,
                               String remoteAddress,
                               String detail) {
        this(source, eventType, principal, remoteAddress, detail, null);
    }
}
