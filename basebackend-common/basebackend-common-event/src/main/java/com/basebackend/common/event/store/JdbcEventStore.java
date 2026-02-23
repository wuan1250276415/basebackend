package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * JDBC 事件存储
 * <p>
 * 基于 JdbcTemplate 实现事件持久化，不依赖特定 ORM。
 * 表结构见 resources/db/event_store.sql。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class JdbcEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcEventStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void save(DomainEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            jdbcTemplate.update(
                    "INSERT INTO domain_event (id, event_type, event_data, status, source, retry_count, max_retries, next_retry_time, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    event.getEventId(),
                    event.getEventType(),
                    eventData,
                    EventStatus.PENDING.name(),
                    event.getSource(),
                    event.getRetryCount(),
                    event.getMaxRetries(),
                    event.getNextRetryTime(),
                    event.getTimestamp(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Failed to save domain event: eventId={}", event.getEventId(), e);
            throw new RuntimeException("Failed to save domain event", e);
        }
    }

    @Override
    public List<DomainEvent> findPendingEvents(int limit) {
        // JDBC 实现返回空列表作为简化实现
        // 因为反序列化 DomainEvent 需要知道具体子类，完整实现需要类型注册机制
        log.debug("findPendingEvents called, limit={}", limit);
        try {
            jdbcTemplate.update(
                    "UPDATE domain_event SET status = ? WHERE status = ? AND (next_retry_time IS NULL OR next_retry_time <= ?) LIMIT ?",
                    EventStatus.PENDING.name(), EventStatus.PENDING.name(), LocalDateTime.now(), limit
            );
        } catch (Exception e) {
            log.warn("Failed to query pending events", e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<DomainEvent> findFailedEvents(int limit) {
        log.debug("findFailedEvents called, limit={}", limit);
        return Collections.emptyList();
    }

    @Override
    public void markAsPublished(String eventId) {
        jdbcTemplate.update(
                "UPDATE domain_event SET status = ?, updated_at = ? WHERE id = ?",
                EventStatus.PUBLISHED.name(), LocalDateTime.now(), eventId
        );
    }

    @Override
    public void markAsFailed(String eventId, String reason) {
        jdbcTemplate.update(
                "UPDATE domain_event SET status = ?, updated_at = ? WHERE id = ?",
                EventStatus.FAILED.name(), LocalDateTime.now(), eventId
        );
    }

    @Override
    public void markAsConsumed(String eventId) {
        jdbcTemplate.update(
                "UPDATE domain_event SET status = ?, updated_at = ? WHERE id = ?",
                EventStatus.CONSUMED.name(), LocalDateTime.now(), eventId
        );
    }

    @Override
    public int deleteExpiredEvents(Duration olderThan) {
        LocalDateTime threshold = LocalDateTime.now().minus(olderThan);
        int deleted = jdbcTemplate.update(
                "DELETE FROM domain_event WHERE created_at < ? AND status IN (?, ?, ?)",
                threshold, EventStatus.PUBLISHED.name(), EventStatus.CONSUMED.name(), EventStatus.FAILED.name()
        );
        if (deleted > 0) {
            log.info("Deleted {} expired domain events older than {}", deleted, threshold);
        }
        return deleted;
    }
}
