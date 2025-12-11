package com.basebackend.messaging.tracing;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息追踪信息
 * <p>
 * 记录消息从生产到消费的完整生命周期。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class MessageTrace {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 追踪ID（分布式追踪）
     */
    private String traceId;

    /**
     * 跨度ID
     */
    private String spanId;

    /**
     * 父跨度ID
     */
    private String parentSpanId;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 生产者ID
     */
    private String producerId;

    /**
     * 消费者组
     */
    private String consumerGroup;

    /**
     * 追踪事件列表
     */
    private List<TraceEvent> events = new ArrayList<>();

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 总耗时（毫秒）
     */
    private Long totalLatencyMs;

    /**
     * 添加追踪事件
     */
    public void addEvent(TraceEvent event) {
        this.events.add(event);
    }

    /**
     * 追踪事件
     */
    @Data
    public static class TraceEvent {
        /**
         * 事件类型
         */
        private EventType eventType;

        /**
         * 事件时间
         */
        private LocalDateTime timestamp;

        /**
         * 耗时（毫秒）
         */
        private Long latencyMs;

        /**
         * 节点信息
         */
        private String node;

        /**
         * 状态
         */
        private String status;

        /**
         * 详细信息
         */
        private String details;

        /**
         * 错误信息
         */
        private String error;

        /**
         * 创建成功事件
         */
        public static TraceEvent success(EventType type, String node, long latencyMs) {
            TraceEvent event = new TraceEvent();
            event.setEventType(type);
            event.setTimestamp(LocalDateTime.now());
            event.setLatencyMs(latencyMs);
            event.setNode(node);
            event.setStatus("SUCCESS");
            return event;
        }

        /**
         * 创建失败事件
         */
        public static TraceEvent failure(EventType type, String node, String error) {
            TraceEvent event = new TraceEvent();
            event.setEventType(type);
            event.setTimestamp(LocalDateTime.now());
            event.setNode(node);
            event.setStatus("FAILURE");
            event.setError(error);
            return event;
        }
    }

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /** 消息创建 */
        MESSAGE_CREATED,
        /** 消息发送 */
        MESSAGE_SENT,
        /** 消息到达Broker */
        MESSAGE_ARRIVED,
        /** 消息投递到消费者 */
        MESSAGE_DELIVERED,
        /** 消息消费开始 */
        CONSUME_STARTED,
        /** 消息消费完成 */
        CONSUME_COMPLETED,
        /** 消息消费失败 */
        CONSUME_FAILED,
        /** 消息重试 */
        MESSAGE_RETRY,
        /** 进入死信队列 */
        TO_DEAD_LETTER
    }
}
