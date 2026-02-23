package com.basebackend.common.event.retry;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件重试调度器
 * <p>
 * 定时扫描 PENDING 和 FAILED 状态的事件，重新发布到 Spring ApplicationEvent。
 * 采用指数退避重试间隔（1s, 2s, 4s, 8s...），超过 maxRetries 后标记为 FAILED 不再重试。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class EventRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventRetryScheduler.class);

    private final EventStore eventStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final int batchSize;

    public EventRetryScheduler(EventStore eventStore,
                               ApplicationEventPublisher applicationEventPublisher,
                               int batchSize) {
        this.eventStore = eventStore;
        this.applicationEventPublisher = applicationEventPublisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${basebackend.common.event.retry.interval-seconds:30}000")
    public void retryPendingEvents() {
        List<DomainEvent> pendingEvents = eventStore.findPendingEvents(batchSize);
        for (DomainEvent event : pendingEvents) {
            retryEvent(event);
        }

        List<DomainEvent> failedEvents = eventStore.findFailedEvents(batchSize);
        for (DomainEvent event : failedEvents) {
            retryEvent(event);
        }
    }

    private void retryEvent(DomainEvent event) {
        if (event.getRetryCount() >= event.getMaxRetries()) {
            eventStore.markAsFailed(event.getEventId(), "Max retries exceeded");
            log.warn("Event {} exceeded max retries ({}), marked as permanently failed",
                    event.getEventId(), event.getMaxRetries());
            return;
        }

        try {
            applicationEventPublisher.publishEvent(event);
            eventStore.markAsPublished(event.getEventId());
            log.info("Event {} retried successfully (attempt {})",
                    event.getEventId(), event.getRetryCount() + 1);
        } catch (Exception e) {
            event.incrementRetryCount();
            // 指数退避：1s, 2s, 4s, 8s...
            long delaySeconds = (long) Math.pow(2, event.getRetryCount() - 1);
            event.setNextRetryTime(LocalDateTime.now().plusSeconds(delaySeconds));
            eventStore.markAsFailed(event.getEventId(), e.getMessage());
            log.warn("Event {} retry failed (attempt {}), next retry in {}s",
                    event.getEventId(), event.getRetryCount(), delaySeconds, e);
        }
    }
}
