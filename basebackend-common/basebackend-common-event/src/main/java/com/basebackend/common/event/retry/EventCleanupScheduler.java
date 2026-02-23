package com.basebackend.common.event.retry;

import com.basebackend.common.event.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * 过期事件清理调度器
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class EventCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventCleanupScheduler.class);

    private final EventStore eventStore;
    private final int expiredDays;

    public EventCleanupScheduler(EventStore eventStore, int expiredDays) {
        this.eventStore = eventStore;
        this.expiredDays = expiredDays;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredEvents() {
        int deleted = eventStore.deleteExpiredEvents(Duration.ofDays(expiredDays));
        if (deleted > 0) {
            log.info("Cleaned up {} expired domain events older than {} days", deleted, expiredDays);
        }
    }
}
