package com.basebackend.common.audit.impl;

import com.basebackend.common.audit.AuditEvent;
import com.basebackend.common.audit.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class SpringAuditEventPublisher implements AuditEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(AuditEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
