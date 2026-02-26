package com.basebackend.common.event;

import com.basebackend.common.event.impl.ReliableDomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DomainEventPublisherTest {

    @Autowired
    private DomainEventPublisher publisher;

    @Autowired
    private TestEventHandler handler;

    @Test
    void publishAndListen() {
        TestEvent event = new TestEvent("test-source");
        publisher.publish(event);

        DomainEvent received = handler.getLastEvent();
        assertNotNull(received);
        assertInstanceOf(TestEvent.class, received);
        assertEquals("test-source", received.getSource());
        assertEquals("TestEvent", received.getEventType());
        assertNotNull(received.getEventId());
        assertNotNull(received.getTimestamp());
    }

    @Test
    void reliableDomainEventPublisherIsUsed() {
        assertInstanceOf(ReliableDomainEventPublisher.class, publisher);
    }

    static class TestEvent extends DomainEvent {
        protected TestEvent(String source) {
            super(source);
        }
    }

    @Component
    static class TestEventHandler {
        private final AtomicReference<DomainEvent> lastEvent = new AtomicReference<>();

        @DomainEventListener
        public void handle(TestEvent event) {
            lastEvent.set(event);
        }

        public DomainEvent getLastEvent() {
            return lastEvent.get();
        }
    }

    @SpringBootApplication
    @Import(TestEventHandler.class)
    static class TestApp {
    }
}
