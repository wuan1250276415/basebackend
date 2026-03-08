package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
    private static final String UPDATE_STATUS_SQL = "UPDATE domain_event SET status = ?, updated_at = ? WHERE id = ?";
    private static final String UPDATE_FAILED_WITH_REASON_SQL =
            "UPDATE domain_event SET status = ?, fail_reason = ?, updated_at = ? WHERE id = ?";
    private static final String QUERY_PENDING_SQL = """
            SELECT id, event_type, event_data, status, source, retry_count, max_retries, next_retry_time, created_at
            FROM domain_event
            WHERE status = ? AND (next_retry_time IS NULL OR next_retry_time <= ?)
            ORDER BY created_at ASC
            LIMIT ?
            """;
    private static final String QUERY_FAILED_SQL = """
            SELECT id, event_type, event_data, status, source, retry_count, max_retries, next_retry_time, created_at
            FROM domain_event
            WHERE status = ? AND retry_count < max_retries
              AND (next_retry_time IS NULL OR next_retry_time <= ?)
            ORDER BY created_at ASC
            LIMIT ?
            """;

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
                    event.getClass().getName(),
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
        try {
            return jdbcTemplate.query(
                    QUERY_PENDING_SQL,
                    (rs, rowNum) -> mapRowToEvent(rs),
                    EventStatus.PENDING.name(),
                    LocalDateTime.now(),
                    limit
            );
        } catch (Exception e) {
            log.warn("Failed to query pending events, limit={}", limit, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DomainEvent> findFailedEvents(int limit) {
        try {
            return jdbcTemplate.query(
                    QUERY_FAILED_SQL,
                    (rs, rowNum) -> mapRowToEvent(rs),
                    EventStatus.FAILED.name(),
                    LocalDateTime.now(),
                    limit
            );
        } catch (Exception e) {
            log.warn("Failed to query failed events, limit={}", limit, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void markAsPublished(String eventId) {
        jdbcTemplate.update(
                UPDATE_STATUS_SQL,
                EventStatus.PUBLISHED.name(), LocalDateTime.now(), eventId
        );
    }

    @Override
    public void markAsFailed(String eventId, String reason) {
        LocalDateTime updatedAt = LocalDateTime.now();
        try {
            jdbcTemplate.update(
                    UPDATE_FAILED_WITH_REASON_SQL,
                    EventStatus.FAILED.name(),
                    reason,
                    updatedAt,
                    eventId
            );
        } catch (DataAccessException e) {
            if (isFailReasonColumnMissing(e)) {
                log.warn("Column fail_reason does not exist, fallback to legacy markAsFailed SQL");
                jdbcTemplate.update(
                        UPDATE_STATUS_SQL,
                        EventStatus.FAILED.name(),
                        updatedAt,
                        eventId
                );
                return;
            }
            throw e;
        }
    }

    @Override
    public void markAsConsumed(String eventId) {
        jdbcTemplate.update(
                UPDATE_STATUS_SQL,
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

    private DomainEvent mapRowToEvent(ResultSet rs) throws SQLException {
        String eventId = rs.getString("id");
        String eventType = rs.getString("event_type");
        String eventData = rs.getString("event_data");
        String source = rs.getString("source");
        int retryCount = rs.getInt("retry_count");
        int maxRetries = rs.getInt("max_retries");
        Timestamp nextRetryTimeTs = rs.getTimestamp("next_retry_time");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        String statusName = rs.getString("status");

        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime nextRetryTime = nextRetryTimeTs != null ? nextRetryTimeTs.toLocalDateTime() : null;
        EventStatus status = parseEventStatus(statusName);

        DomainEvent event = deserializeEvent(eventType, eventData, source);
        applyEventMetadata(event, eventId, eventType, source, createdAt, retryCount, maxRetries, nextRetryTime, status);
        return event;
    }

    private EventStatus parseEventStatus(String statusName) {
        if (statusName == null || statusName.isBlank()) {
            return EventStatus.PENDING;
        }
        try {
            return EventStatus.valueOf(statusName);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown event status '{}', fallback to PENDING", statusName);
            return EventStatus.PENDING;
        }
    }

    private DomainEvent deserializeEvent(String eventType, String eventData, String source) {
        Class<? extends DomainEvent> eventClass = resolveEventClass(eventType);
        if (eventClass != null) {
            try {
                return objectMapper.readValue(eventData, eventClass);
            } catch (Exception e) {
                log.warn("Failed to deserialize domain event by class '{}', fallback to persisted event wrapper", eventType, e);
            }
        }
        return new PersistedDomainEvent(source, eventData, eventType);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends DomainEvent> resolveEventClass(String eventType) {
        if (eventType == null || eventType.isBlank() || !eventType.contains(".")) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(eventType);
            if (!DomainEvent.class.isAssignableFrom(clazz)) {
                log.warn("Event type '{}' is not a DomainEvent subtype", eventType);
                return null;
            }
            return (Class<? extends DomainEvent>) clazz;
        } catch (ClassNotFoundException e) {
            log.warn("Event class '{}' not found", eventType);
            return null;
        }
    }

    private void applyEventMetadata(DomainEvent event,
                                    String eventId,
                                    String eventType,
                                    String source,
                                    LocalDateTime createdAt,
                                    int retryCount,
                                    int maxRetries,
                                    LocalDateTime nextRetryTime,
                                    EventStatus status) {
        setDomainEventField(event, "eventId", eventId);
        setDomainEventField(event, "eventType", eventType);
        setDomainEventField(event, "timestamp", createdAt);
        setDomainEventField(event, "source", source);
        setDomainEventField(event, "retryCount", retryCount);
        setDomainEventField(event, "maxRetries", maxRetries);
        setDomainEventField(event, "nextRetryTime", nextRetryTime);
        setDomainEventField(event, "status", status);
    }

    private boolean isFailReasonColumnMissing(DataAccessException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ("42S22".equals(sqlState) || "42703".equals(sqlState) || sqlException.getErrorCode() == 1054) {
                    return true;
                }
            }
            current = current.getCause();
        }
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("fail_reason")
                && (normalizedMessage.contains("unknown column")
                || normalizedMessage.contains("does not exist")
                || normalizedMessage.contains("invalid column"));
    }

    private void setDomainEventField(DomainEvent event, String fieldName, Object value) {
        try {
            Field field = DomainEvent.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(event, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to set domain event field: " + fieldName, e);
        }
    }

    /**
     * JDBC 反序列化降级事件包装类。
     */
    private static final class PersistedDomainEvent extends DomainEvent {
        @SuppressWarnings("unused")
        private final String payload;

        @SuppressWarnings("unused")
        private final String declaredEventType;

        private PersistedDomainEvent(String source, String payload, String declaredEventType) {
            super(source);
            this.payload = payload;
            this.declaredEventType = declaredEventType;
        }
    }
}
