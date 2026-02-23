package com.basebackend.common.event;

/**
 * 事件状态枚举
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public enum EventStatus {

    /**
     * 待发布
     */
    PENDING,

    /**
     * 已发布（已投递到 ApplicationEventPublisher）
     */
    PUBLISHED,

    /**
     * 已消费
     */
    CONSUMED,

    /**
     * 发布失败
     */
    FAILED
}
