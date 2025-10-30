package com.basebackend.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一消息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID
     */
    private String messageId;

    /**
     * 消息主题（Topic）
     */
    private String topic;

    /**
     * 路由键（RabbitMQ）/ 标签（RocketMQ）
     */
    private String routingKey;

    /**
     * 消息标签（RocketMQ Tag，用于消息过滤）
     */
    private String tags;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息体
     */
    private T payload;

    /**
     * 消息头（扩展属性）
     */
    private Map<String, Object> headers;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 延迟时间（毫秒）
     */
    private Long delayMillis;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 顺序消息分区键（同一分区键的消息保证顺序）
     */
    private String partitionKey;

    /**
     * 是否需要事务保障
     */
    private Boolean transactional;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 追踪ID
     */
    private String traceId;
}
