package com.basebackend.messaging.constants;

/**
 * RocketMQ 常量
 *
 * @author Claude Code
 * @since 2025-10-30
 */
public class RocketMQConstants {

    /**
     * 默认 Topic
     */
    public static final String DEFAULT_TOPIC = "basebackend-topic";

    /**
     * 事件 Topic
     */
    public static final String EVENT_TOPIC = "basebackend-event-topic";

    /**
     * 死信 Topic
     */
    public static final String DLQ_TOPIC = "basebackend-dlq-topic";

    /**
     * 顺序消息 Topic
     */
    public static final String ORDERED_TOPIC = "basebackend-ordered-topic";

    /**
     * 默认消费者组
     */
    public static final String DEFAULT_CONSUMER_GROUP = "basebackend-consumer-group";

    /**
     * 死信消费者组
     */
    public static final String DLQ_CONSUMER_GROUP = "basebackend-dlq-consumer-group";

    /**
     * 默认生产者组
     */
    public static final String DEFAULT_PRODUCER_GROUP = "basebackend-producer-group";

    /**
     * Tag分隔符
     */
    public static final String TAG_SEPARATOR = "||";

    /**
     * 延迟级别映射（毫秒 -> 延迟级别）
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     */
    public static int getDelayLevel(long delayMillis) {
        if (delayMillis <= 1000) return 1;        // 1s
        if (delayMillis <= 5000) return 2;        // 5s
        if (delayMillis <= 10000) return 3;       // 10s
        if (delayMillis <= 30000) return 4;       // 30s
        if (delayMillis <= 60000) return 5;       // 1m
        if (delayMillis <= 120000) return 6;      // 2m
        if (delayMillis <= 180000) return 7;      // 3m
        if (delayMillis <= 240000) return 8;      // 4m
        if (delayMillis <= 300000) return 9;      // 5m
        if (delayMillis <= 360000) return 10;     // 6m
        if (delayMillis <= 420000) return 11;     // 7m
        if (delayMillis <= 480000) return 12;     // 8m
        if (delayMillis <= 540000) return 13;     // 9m
        if (delayMillis <= 600000) return 14;     // 10m
        if (delayMillis <= 1200000) return 15;    // 20m
        if (delayMillis <= 1800000) return 16;    // 30m
        if (delayMillis <= 3600000) return 17;    // 1h
        return 18;                                 // 2h
    }
}
