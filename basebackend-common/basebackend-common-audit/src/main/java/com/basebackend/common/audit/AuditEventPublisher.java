package com.basebackend.common.audit;

public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
