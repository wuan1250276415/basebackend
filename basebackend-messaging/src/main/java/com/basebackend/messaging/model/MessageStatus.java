package com.basebackend.messaging.model;

/**
 * 消息状态枚举
 */
public enum MessageStatus {
    /**
     * 待发送
     */
    PENDING,

    /**
     * 已发送
     */
    SENT,

    /**
     * 已投递
     */
    DELIVERED,

    /**
     * 消费中
     */
    CONSUMING,

    /**
     * 消费成功
     */
    CONSUMED,

    /**
     * 消费失败
     */
    FAILED,

    /**
     * 死信
     */
    DEAD_LETTER
}
