package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private static final String SELECT_PENDING_SQL =
            "SELECT id, event_type, event_data, source, retry_count, max_retries, status, created_at, next_retry_time "
                    + "FROM domain_event WHERE status = ? AND (next_retry_time IS NULL OR next_retry_time <= ?) "
                    + "ORDER BY created_at ASC LIMIT ?";
    private static final String SELECT_FAILED_SQL =
            "SELECT id, event_type, event_data, source, retry_count, max_retries, status, created_at, next_retry_time "
                    + "FROM domain_event WHERE status = ? AND retry_count < max_retries "
                    + "AND (next_retry_time IS NULL OR next_retry_time <= ?) "
                    + "ORDER BY next_retry_time ASC, created_at ASC LIMIT ?";
    private static final String UPDATE_STATUS_SQL =
            "UPDATE domain_event SET status = ?, updated_at = ? WHERE id = ?";
    private static final String UPDATE_FAILED_WITH_REASON_SQL =
            "UPDATE domain_event SET status = ?, fail_reason = ?, updated_at = ? WHERE id = ?";

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
        return jdbcTemplate.query(
                SELECT_PENDING_SQL,
                eventRowMapper(),
                EventStatus.PENDING.name(),
                LocalDateTime.now(),
                limit
        );
    }

    @Override
    public List<DomainEvent> findFailedEvents(int limit) {
        return jdbcTemplate.query(
                SELECT_FAILED_SQL,
                eventRowMapper(),
                EventStatus.FAILED.name(),
                LocalDateTime.now(),
                limit
        );
    }

    @Override
    public void markAsPublished(String eventId) {
        jdbcTemplate.update(UPDATE_STATUS_SQL, EventStatus.PUBLISHED.name(), LocalDateTime.now(), eventId);
    }

    @Override
    public void markAsFailed(String eventId, String reason) {
        try {
            jdbcTemplate.update(
                    UPDATE_FAILED_WITH_REASON_SQL,
                    EventStatus.FAILED.name(), reason, LocalDateTime.now(), eventId
            );
        } catch (BadSqlGrammarException ex) {
            if (isMissingFailReasonColumn(ex)) {
                log.warn("domain_event 缺少 fail_reason 列，回退到兼容 SQL: eventId={}", eventId);
                jdbcTemplate.update(UPDATE_STATUS_SQL, EventStatus.FAILED.name(), LocalDateTime.now(), eventId);
                return;
            }
            throw ex;
        }
    }

    @Override
    public void markAsConsumed(String eventId) {
        jdbcTemplate.update(UPDATE_STATUS_SQL, EventStatus.CONSUMED.name(), LocalDateTime.now(), eventId);
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

    private RowMapper<DomainEvent> eventRowMapper() {
        return (rs, rowNum) -> mapEventRow(rs);
    }

    private DomainEvent mapEventRow(ResultSet rs) throws SQLException {
        String eventId = rs.getString("id");
        String eventType = rs.getString("event_type");
        String eventData = rs.getString("event_data");
        String source = rs.getString("source");
        int retryCount = rs.getInt("retry_count");
        int maxRetries = rs.getInt("max_retries");
        EventStatus status = EventStatus.valueOf(rs.getString("status"));
        LocalDateTime createdAt = toLocalDateTime(rs.getTimestamp("created_at"));
        Timestamp nextRetryTimestamp = rs.getTimestamp("next_retry_time");
        LocalDateTime nextRetryTime = nextRetryTimestamp != null ? nextRetryTimestamp.toLocalDateTime() : null;

        DomainEvent event = deserializeEvent(eventType, eventData, source);
        restoreStoredState(event, eventId, eventType, createdAt, source, retryCount, maxRetries, status, nextRetryTime);
        return event;
    }

    private DomainEvent deserializeEvent(String eventType, String eventData, String source) {
        try {
            Class<?> rawClass = Class.forName(eventType);
            if (DomainEvent.class.isAssignableFrom(rawClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends DomainEvent> eventClass = (Class<? extends DomainEvent>) rawClass;
                return objectMapper.readValue(eventData, eventClass);
            }
        } catch (ClassNotFoundException e) {
            log.warn("事件类不存在，使用降级包装事件: eventType={}", eventType);
        } catch (Exception e) {
            log.warn("事件反序列化失败，使用降级包装事件: eventType={}", eventType, e);
        }
        return new RehydratedDomainEvent(source, eventData);
    }

    private void restoreStoredState(DomainEvent event, String eventId, String eventType,
                                    LocalDateTime timestamp, String source,
                                    int retryCount, int maxRetries,
                                    EventStatus status, LocalDateTime nextRetryTime) {
        setField(event, "eventId", eventId);
        setField(event, "eventType", eventType);
        setField(event, "timestamp", timestamp);
        setField(event, "source", source);
        setField(event, "retryCount", retryCount);
        event.setMaxRetries(maxRetries);
        event.setStatus(status);
        event.setNextRetryTime(nextRetryTime);
    }

    private void setField(DomainEvent target, String fieldName, Object value) {
        try {
            Class<?> current = target.getClass();
            while (current != null) {
                try {
                    var field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to restore event field: " + fieldName, e);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private boolean isMissingFailReasonColumn(DataAccessException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SQLException sqlException) {
            String message = sqlException.getMessage();
            return sqlException.getErrorCode() == 1054
                    || "42S22".equals(sqlException.getSQLState())
                    || (message != null && message.contains("fail_reason"));
        }
        String message = ex.getMessage();
        return message != null && message.contains("fail_reason");
    }

    private static final class RehydratedDomainEvent extends DomainEvent {
        private final String rawEventData;

        private RehydratedDomainEvent(String source, String rawEventData) {
            super(source);
            this.rawEventData = rawEventData;
        }

        public String getRawEventData() {
            return rawEventData;
        }
    }
}
