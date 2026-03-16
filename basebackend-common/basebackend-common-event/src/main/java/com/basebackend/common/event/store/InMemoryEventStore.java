package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存事件存储
 * <p>
 * 基于 ConcurrentHashMap 的降级方案。
 * 服务重启后事件丢失，仅适用于开发/测试环境。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class InMemoryEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);

    private final Map<String, EventEntry> events = new ConcurrentHashMap<>();

    @Override
    public void save(DomainEvent event) {
        events.put(event.getEventId(), new EventEntry(event, EventStatus.PENDING, null, LocalDateTime.now()));
    }

    @Override
    public List<DomainEvent> findPendingEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return events.values().stream()
                .filter(e -> {
                    synchronized (e) {
                        return e.status == EventStatus.PENDING;
                    }
                })
                .filter(e -> e.event.getNextRetryTime() == null || !e.event.getNextRetryTime().isAfter(now))
                .limit(limit)
                .map(e -> e.event)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainEvent> findFailedEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return events.values().stream()
                .filter(e -> {
                    synchronized (e) {
                        return e.status == EventStatus.FAILED;
                    }
                })
                .filter(e -> e.event.getRetryCount() < e.event.getMaxRetries())
                .filter(e -> e.event.getNextRetryTime() == null || !e.event.getNextRetryTime().isAfter(now))
                .limit(limit)
                .map(e -> e.event)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsPublished(String eventId) {
        EventEntry entry = events.get(eventId);
        if (entry != null) {
            synchronized (entry) {
                entry.status = EventStatus.PUBLISHED;
            }
        }
    }

    @Override
    public void markAsFailed(String eventId, String reason) {
        EventEntry entry = events.get(eventId);
        if (entry != null) {
            synchronized (entry) {
                entry.status = EventStatus.FAILED;
                entry.failReason = reason;
            }
        }
    }

    @Override
    public void markAsConsumed(String eventId) {
        EventEntry entry = events.get(eventId);
        if (entry != null) {
            synchronized (entry) {
                entry.status = EventStatus.CONSUMED;
            }
        }
    }

    @Override
    public int deleteExpiredEvents(Duration olderThan) {
        LocalDateTime threshold = LocalDateTime.now().minus(olderThan);
        int before = events.size();
        events.entrySet().removeIf(e ->
                e.getValue().createdAt.isBefore(threshold)
                        && (e.getValue().status == EventStatus.PUBLISHED
                        || e.getValue().status == EventStatus.CONSUMED
                        || e.getValue().status == EventStatus.FAILED));
        int removed = before - events.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired events from in-memory store", removed);
        }
        return removed;
    }

    private static class EventEntry {
        final DomainEvent event;
        volatile EventStatus status;
        volatile String failReason;
        final LocalDateTime createdAt;

        EventEntry(DomainEvent event, EventStatus status, String failReason, LocalDateTime createdAt) {
            this.event = event;
            this.status = status;
            this.failReason = failReason;
            this.createdAt = createdAt;
        }
    }
}
