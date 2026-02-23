package com.basebackend.common.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 * <p>
 * 所有领域事件的抽象基类，提供事件标识、类型、时间戳、来源等基本信息，
 * 以及可靠发布所需的重试状态字段。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Getter
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final LocalDateTime timestamp;
    private final String source;

    /**
     * 事件状态
     */
    @Setter
    private EventStatus status = EventStatus.PENDING;

    /**
     * 已重试次数
     */
    private int retryCount = 0;

    /**
     * 最大重试次数
     */
    @Setter
    private int maxRetries = 3;

    /**
     * 下次重试时间
     */
    @Setter
    private LocalDateTime nextRetryTime;

    protected DomainEvent(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.timestamp = LocalDateTime.now();
        this.source = source;
    }

    /**
     * 增加重试计数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
}
