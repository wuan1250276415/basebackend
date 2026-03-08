package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JdbcEventStore 单元测试
 */
class JdbcEventStoreTest {

    private static final String UPDATE_STATUS_SQL = "UPDATE domain_event SET status = ?, updated_at = ? WHERE id = ?";
    private static final String UPDATE_FAILED_WITH_REASON_SQL =
            "UPDATE domain_event SET status = ?, fail_reason = ?, updated_at = ? WHERE id = ?";

    @Test
    @DisplayName("findPendingEvents 应返回可反序列化事件")
    void shouldReturnPendingEvents() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcEventStore eventStore = new JdbcEventStore(jdbcTemplate);

        String eventType = JdbcTestEvent.class.getName();
        JdbcTestEvent event = new JdbcTestEvent("unit-test", "demo");
        String eventData = toJson(event);
        String eventId = "event-001";
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 6, 10, 0, 0);

        mockQueryResult(jdbcTemplate, eventId, eventType, eventData, "unit-test", 1, 3, createdAt, null, EventStatus.PENDING);

        List<DomainEvent> results = eventStore.findPendingEvents(10);
        assertThat(results).hasSize(1);

        DomainEvent loaded = results.get(0);
        assertThat(loaded).isInstanceOf(JdbcTestEvent.class);
        assertThat(loaded.getEventId()).isEqualTo(eventId);
        assertThat(loaded.getEventType()).isEqualTo(eventType);
        assertThat(loaded.getSource()).isEqualTo("unit-test");
        assertThat(loaded.getRetryCount()).isEqualTo(1);
        assertThat(loaded.getMaxRetries()).isEqualTo(3);
        assertThat(loaded.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(loaded.getTimestamp()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("findFailedEvents 类不存在时应降级返回包装事件")
    void shouldFallbackWhenEventClassMissing() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcEventStore eventStore = new JdbcEventStore(jdbcTemplate);

        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 6, 11, 0, 0);
        LocalDateTime nextRetry = LocalDateTime.of(2026, 3, 6, 11, 5, 0);

        mockQueryResult(
                jdbcTemplate,
                "event-404",
                "com.basebackend.notexists.MissingEvent",
                "{\"foo\":\"bar\"}",
                "fallback-source",
                2,
                5,
                createdAt,
                nextRetry,
                EventStatus.FAILED
        );

        List<DomainEvent> results = eventStore.findFailedEvents(10);
        assertThat(results).hasSize(1);

        DomainEvent loaded = results.get(0);
        assertThat(loaded.getEventId()).isEqualTo("event-404");
        assertThat(loaded.getEventType()).isEqualTo("com.basebackend.notexists.MissingEvent");
        assertThat(loaded.getSource()).isEqualTo("fallback-source");
        assertThat(loaded.getRetryCount()).isEqualTo(2);
        assertThat(loaded.getMaxRetries()).isEqualTo(5);
        assertThat(loaded.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(loaded.getNextRetryTime()).isEqualTo(nextRetry);
    }

    @Test
    @DisplayName("markAsFailed 应落库失败原因")
    void shouldPersistFailReasonWhenMarkAsFailed() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcEventStore eventStore = new JdbcEventStore(jdbcTemplate);

        eventStore.markAsFailed("event-500", "网络超时");

        verify(jdbcTemplate).update(
                eq(UPDATE_FAILED_WITH_REASON_SQL),
                eq(EventStatus.FAILED.name()),
                eq("网络超时"),
                any(LocalDateTime.class),
                eq("event-500")
        );
    }

    @Test
    @DisplayName("markAsFailed 缺少 fail_reason 列时应回退老 SQL")
    void shouldFallbackToLegacySqlWhenFailReasonColumnMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcEventStore eventStore = new JdbcEventStore(jdbcTemplate);
        SQLException sqlException = new SQLException("Unknown column 'fail_reason' in 'field list'", "42S22", 1054);
        when(jdbcTemplate.update(
                eq(UPDATE_FAILED_WITH_REASON_SQL),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(new BadSqlGrammarException("markAsFailed", UPDATE_FAILED_WITH_REASON_SQL, sqlException));

        eventStore.markAsFailed("legacy-event", "旧库缺列");

        verify(jdbcTemplate).update(
                eq(UPDATE_STATUS_SQL),
                eq(EventStatus.FAILED.name()),
                any(LocalDateTime.class),
                eq("legacy-event")
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockQueryResult(JdbcTemplate jdbcTemplate,
                                 String eventId,
                                 String eventType,
                                 String eventData,
                                 String source,
                                 int retryCount,
                                 int maxRetries,
                                 LocalDateTime createdAt,
                                 LocalDateTime nextRetryTime,
                                 EventStatus status) throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenAnswer(invocation -> {
                    RowMapper<DomainEvent> mapper = (RowMapper<DomainEvent>) invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("id")).thenReturn(eventId);
                    when(rs.getString("event_type")).thenReturn(eventType);
                    when(rs.getString("event_data")).thenReturn(eventData);
                    when(rs.getString("source")).thenReturn(source);
                    when(rs.getInt("retry_count")).thenReturn(retryCount);
                    when(rs.getInt("max_retries")).thenReturn(maxRetries);
                    when(rs.getString("status")).thenReturn(status.name());
                    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(createdAt));
                    when(rs.getTimestamp("next_retry_time"))
                            .thenReturn(nextRetryTime == null ? null : Timestamp.valueOf(nextRetryTime));
                    return List.of(mapper.mapRow(rs, 0));
                });
    }

    private String toJson(Object value) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.writeValueAsString(value);
    }

    static class JdbcTestEvent extends DomainEvent {
        private final String name;

        @JsonCreator
        JdbcTestEvent(@JsonProperty("source") String source,
                      @JsonProperty("name") String name) {
            super(source);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
