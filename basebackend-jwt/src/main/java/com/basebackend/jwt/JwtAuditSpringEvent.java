package com.basebackend.jwt;

import org.springframework.context.ApplicationEvent;

/**
 * JWT 审计 Spring 事件 — 通过 ApplicationEventPublisher 发布，
 * 便于上层扩展（存数据库/Kafka 等）
 */
public class JwtAuditSpringEvent extends ApplicationEvent {

    private final JwtAuditEntry auditEntry;

    public JwtAuditSpringEvent(Object source, JwtAuditEntry auditEntry) {
        super(source);
        this.auditEntry = auditEntry;
    }

    public JwtAuditEntry getAuditEntry() {
        return auditEntry;
    }
}
