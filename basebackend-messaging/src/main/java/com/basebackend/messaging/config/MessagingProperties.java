package com.basebackend.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {

    /**
     * RocketMQ配置
     */
    private RocketMQ rocketmq = new RocketMQ();

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * 死信队列配置
     */
    private DeadLetter deadLetter = new DeadLetter();

    /**
     * 事务消息配置
     */
    private Transaction transaction = new Transaction();

    /**
     * 幂等性配置
     */
    private Idempotency idempotency = new Idempotency();

    @Data
    public static class RocketMQ {
        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 默认 Topic
         */
        private String defaultTopic = "basebackend-topic";

        /**
         * Topic 前缀
         */
        private String topicPrefix = "basebackend.";
    }

    @Data
    public static class Retry {
        /**
         * 是否启用重试
         */
        private Boolean enabled = true;

        /**
         * 最大重试次数（RocketMQ 默认 16 次）
         */
        private Integer maxAttempts = 16;
    }

    @Data
    public static class DeadLetter {
        /**
         * 是否启用死信队列
         */
        private Boolean enabled = true;

        /**
         * 死信 Topic
         */
        private String topic = "basebackend-dlq-topic";

        /**
         * 死信消费者组
         */
        private String consumerGroup = "basebackend-dlq-consumer-group";
    }

    @Data
    public static class Transaction {
        /**
         * 是否启用事务消息
         */
        private Boolean enabled = true;

        /**
         * 本地消息表名
         */
        private String tableName = "sys_message_log";

        /**
         * 消息补偿检查间隔（秒）
         */
        private Long checkInterval = 60L;

        /**
         * 消息超时时间（分钟）
         */
        private Long timeout = 30L;
    }

    @Data
    public static class Idempotency {
        /**
         * 是否启用幂等性
         */
        private Boolean enabled = true;

        /**
         * 幂等性缓存过期时间（秒）
         */
        private Long expireTime = 3600L;

        /**
         * Redis键前缀
         */
        private String keyPrefix = "msg:idempotent:";
    }
}
