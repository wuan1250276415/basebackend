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
        // JDBC 模式下需要具体子类类型注册才能完成反序列化，当前版本暂不支持。
        // 自动事件重试在 JDBC 存储模式下不可用，请改用 RedisEventStore 或实现自定义 EventStore。
        // 若确实需要 JDBC 重试支持，请子类化本类并重写此方法以提供类型注册逻辑。
        log.warn("JdbcEventStore 不支持自动事件重试（findPendingEvents），如需此功能请配置 Redis 事件存储");
        return Collections.emptyList();
    }

    @Override
    public List<DomainEvent> findFailedEvents(int limit) {
        // 同 findPendingEvents，JDBC 模式下不支持失败事件自动恢复。
        log.warn("JdbcEventStore 不支持失败事件自动恢复（findFailedEvents），如需此功能请配置 Redis 事件存储");
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
