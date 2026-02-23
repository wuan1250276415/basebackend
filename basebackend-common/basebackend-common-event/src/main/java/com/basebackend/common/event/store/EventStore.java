package com.basebackend.common.event.store;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.EventStatus;

import java.time.Duration;
import java.util.List;

/**
 * 事件持久化 SPI
 * <p>
 * 提供事件的存储、查询、状态变更和过期清理能力。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface EventStore {

    /**
     * 保存事件
     */
    void save(DomainEvent event);

    /**
     * 查询待发布的事件
     */
    List<DomainEvent> findPendingEvents(int limit);

    /**
     * 查询失败的事件（可重试）
     */
    List<DomainEvent> findFailedEvents(int limit);

    /**
     * 标记事件为已发布
     */
    void markAsPublished(String eventId);

    /**
     * 标记事件为失败
     */
    void markAsFailed(String eventId, String reason);

    /**
     * 标记事件为已消费
     */
    void markAsConsumed(String eventId);

    /**
     * 删除过期事件
     *
     * @param olderThan 超过此时长的事件将被删除
     * @return 删除的事件数
     */
    int deleteExpiredEvents(Duration olderThan);
}
