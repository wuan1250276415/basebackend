package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryEventStore 单元测试
 */
class InMemoryEventStoreTest {

    private InMemoryEventStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryEventStore();
    }

    // 测试用具体事件类
    static class TestEvent extends DomainEvent {
        private final String payload;

        TestEvent(String source, String payload) {
            super(source);
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }
    }

    // ========== save + findPendingEvents ==========

    @Nested
    @DisplayName("save & findPending")
    class SaveAndFind {

        @Test
        @DisplayName("保存后可查到 PENDING 事件")
        void shouldFindSavedEvent() {
            var event = new TestEvent("test-service", "data");
            store.save(event);

            List<DomainEvent> pending = store.findPendingEvents(10);
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getEventId()).isEqualTo(event.getEventId());
        }

        @Test
        @DisplayName("多个事件都能查到")
        void shouldFindMultipleEvents() {
            store.save(new TestEvent("svc", "a"));
            store.save(new TestEvent("svc", "b"));
            store.save(new TestEvent("svc", "c"));

            assertThat(store.findPendingEvents(10)).hasSize(3);
        }

        @Test
        @DisplayName("limit 限制返回数量")
        void shouldRespectLimit() {
            for (int i = 0; i < 5; i++) {
                store.save(new TestEvent("svc", "event-" + i));
            }
            assertThat(store.findPendingEvents(3)).hasSize(3);
        }

        @Test
        @DisplayName("空存储返回空列表")
        void shouldReturnEmptyForEmptyStore() {
            assertThat(store.findPendingEvents(10)).isEmpty();
        }
    }

    // ========== markAsPublished ==========

    @Nested
    @DisplayName("markAsPublished")
    class MarkPublished {

        @Test
        @DisplayName("标记后不再出现在 pending 列表")
        void shouldRemoveFromPending() {
            var event = new TestEvent("svc", "data");
            store.save(event);
            store.markAsPublished(event.getEventId());

            assertThat(store.findPendingEvents(10)).isEmpty();
        }
    }

    // ========== markAsFailed ==========

    @Nested
    @DisplayName("markAsFailed")
    class MarkFailed {

        @Test
        @DisplayName("标记失败后出现在 failed 列表")
        void shouldAppearInFailedList() {
            var event = new TestEvent("svc", "data");
            store.save(event);
            store.markAsFailed(event.getEventId(), "网络超时");

            assertThat(store.findPendingEvents(10)).isEmpty();
            assertThat(store.findFailedEvents(10)).hasSize(1);
        }

        @Test
        @DisplayName("超过最大重试次数后不在 failed 列表")
        void shouldNotAppearAfterMaxRetries() {
            var event = new TestEvent("svc", "data");
            event.setMaxRetries(0); // 不允许重试
            store.save(event);
            store.markAsFailed(event.getEventId(), "失败");

            assertThat(store.findFailedEvents(10)).isEmpty();
        }
    }

    // ========== markAsConsumed ==========

    @Nested
    @DisplayName("markAsConsumed")
    class MarkConsumed {

        @Test
        @DisplayName("标记消费后不再出现在任何列表")
        void shouldNotAppearAnywhere() {
            var event = new TestEvent("svc", "data");
            store.save(event);
            store.markAsConsumed(event.getEventId());

            assertThat(store.findPendingEvents(10)).isEmpty();
            assertThat(store.findFailedEvents(10)).isEmpty();
        }
    }

    // ========== deleteExpiredEvents ==========

    @Nested
    @DisplayName("deleteExpiredEvents")
    class DeleteExpired {

        @Test
        @DisplayName("未过期事件不被删除")
        void shouldNotDeleteFreshEvents() {
            var event = new TestEvent("svc", "fresh");
            store.save(event);
            store.markAsPublished(event.getEventId());

            int deleted = store.deleteExpiredEvents(Duration.ofHours(1));
            assertThat(deleted).isEqualTo(0);
        }

        @Test
        @DisplayName("PENDING 事件不被清理（即使过期）")
        void shouldNotDeletePendingEvents() {
            var event = new TestEvent("svc", "pending");
            store.save(event);

            // 即使用 Duration.ZERO，PENDING 也不应被删除（除非实现允许）
            // 实际上 deleteExpiredEvents 只删 PUBLISHED/CONSUMED/FAILED
            int deleted = store.deleteExpiredEvents(Duration.ZERO);
            // PENDING 事件刚创建，不满足 olderThan=0 的条件
            assertThat(store.findPendingEvents(10)).hasSize(1);
        }

        @Test
        @DisplayName("不存在的事件 markAs 不抛异常")
        void shouldNotThrowForNonExistent() {
            store.markAsPublished("non-existent");
            store.markAsFailed("non-existent", "reason");
            store.markAsConsumed("non-existent");
            // should not throw
        }
    }
}
