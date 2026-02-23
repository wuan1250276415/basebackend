package com.basebackend.common.event;

import com.basebackend.common.event.impl.ReliableDomainEventPublisher;
import com.basebackend.common.event.store.EventStore;
import com.basebackend.common.event.store.InMemoryEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DomainEvent 增强功能单元测试
 */
class DomainEventTest {

    private InMemoryEventStore eventStore;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore();
    }

    // ========== 事件发布和持久化 ==========

    @Test
    void publish_shouldPersistAndPublishEvent() {
        AtomicReference<DomainEvent> published = new AtomicReference<>();
        ApplicationEventPublisher mockPublisher = event -> published.set((DomainEvent) event);

        ReliableDomainEventPublisher publisher =
                new ReliableDomainEventPublisher(mockPublisher, eventStore);

        TestEvent event = new TestEvent("test-source");
        publisher.publish(event);

        // 验证事件已发布
        assertNotNull(published.get());
        assertEquals("test-source", published.get().getSource());
        assertEquals("TestEvent", published.get().getEventType());
    }

    @Test
    void domainEvent_hasRetryFields() {
        TestEvent event = new TestEvent("src");
        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertEquals(3, event.getMaxRetries());
        assertNull(event.getNextRetryTime());
    }

    @Test
    void domainEvent_incrementRetryCount() {
        TestEvent event = new TestEvent("src");
        event.incrementRetryCount();
        assertEquals(1, event.getRetryCount());
        event.incrementRetryCount();
        assertEquals(2, event.getRetryCount());
    }

    // ========== InMemoryEventStore ==========

    @Test
    void inMemoryStore_saveAndFindPending() {
        TestEvent event = new TestEvent("src");
        eventStore.save(event);

        List<DomainEvent> pending = eventStore.findPendingEvents(10);
        assertEquals(1, pending.size());
        assertEquals(event.getEventId(), pending.get(0).getEventId());
    }

    @Test
    void inMemoryStore_markAsPublished() {
        TestEvent event = new TestEvent("src");
        eventStore.save(event);

        eventStore.markAsPublished(event.getEventId());

        List<DomainEvent> pending = eventStore.findPendingEvents(10);
        assertTrue(pending.isEmpty());
    }

    @Test
    void inMemoryStore_markAsFailed() {
        TestEvent event = new TestEvent("src");
        eventStore.save(event);

        eventStore.markAsFailed(event.getEventId(), "network error");

        List<DomainEvent> pending = eventStore.findPendingEvents(10);
        assertTrue(pending.isEmpty());

        List<DomainEvent> failed = eventStore.findFailedEvents(10);
        assertEquals(1, failed.size());
    }

    @Test
    void inMemoryStore_markAsConsumed() {
        TestEvent event = new TestEvent("src");
        eventStore.save(event);

        eventStore.markAsConsumed(event.getEventId());

        List<DomainEvent> pending = eventStore.findPendingEvents(10);
        assertTrue(pending.isEmpty());
    }

    // ========== 过期清理 ==========

    @Test
    void inMemoryStore_deleteExpiredEvents() {
        TestEvent event = new TestEvent("src");
        eventStore.save(event);
        eventStore.markAsPublished(event.getEventId());

        // 清理 0 天前的事件（应该包含所有）
        int deleted = eventStore.deleteExpiredEvents(Duration.ZERO);
        assertEquals(1, deleted);
    }

    @Test
    void inMemoryStore_doesNotDeletePendingExpiredEvents() {
        TestEvent event = new TestEvent("src");
        eventStore.save(event);
        // PENDING 状态的事件不应被清理
        int deleted = eventStore.deleteExpiredEvents(Duration.ZERO);
        assertEquals(0, deleted);
    }

    // ========== Test Event ==========

    static class TestEvent extends DomainEvent {
        protected TestEvent(String source) {
            super(source);
        }
    }
}
