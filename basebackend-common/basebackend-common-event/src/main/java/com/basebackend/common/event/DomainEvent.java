package com.basebackend.common.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final LocalDateTime timestamp;
    private final String source;

    protected DomainEvent(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.timestamp = LocalDateTime.now();
        this.source = source;
    }
}
