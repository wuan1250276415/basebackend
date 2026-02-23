package com.basebackend.common.event.impl;

import com.basebackend.common.event.DomainEvent;
import com.basebackend.common.event.DomainEventPublisher;
import com.basebackend.common.event.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 可靠域事件发布器
 * <p>
 * 增强版发布器，先持久化到 {@link EventStore}（PENDING），
 * 再发布到 Spring {@link ApplicationEventPublisher}，成功后标记为 PUBLISHED。
 * 如果在事务中，注册 {@link TransactionSynchronization#afterCommit()} 回调，
 * 确保事务提交后才发布事件。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class ReliableDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReliableDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EventStore eventStore;

    public ReliableDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                        EventStore eventStore) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventStore = eventStore;
    }

    @Override
    public void publish(DomainEvent event) {
        // 1. 先持久化（PENDING 状态）
        eventStore.save(event);

        // 2. 判断是否在事务中
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 事务内：注册 afterCommit 回调
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(event);
                }
            });
        } else {
            // 非事务：直接发布
            doPublish(event);
        }
    }

    private void doPublish(DomainEvent event) {
        try {
            applicationEventPublisher.publishEvent(event);
            eventStore.markAsPublished(event.getEventId());
            log.debug("Domain event published: eventId={}, type={}", event.getEventId(), event.getEventType());
        } catch (Exception e) {
            eventStore.markAsFailed(event.getEventId(), e.getMessage());
            log.error("Failed to publish domain event: eventId={}, type={}", event.getEventId(), event.getEventType(), e);
        }
    }
}
