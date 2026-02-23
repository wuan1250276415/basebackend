package com.basebackend.common.event;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
