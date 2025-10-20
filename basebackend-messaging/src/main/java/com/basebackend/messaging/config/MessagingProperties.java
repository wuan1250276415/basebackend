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
     * RabbitMQ配置
     */
    private RabbitMQ rabbitmq = new RabbitMQ();

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
    public static class RabbitMQ {
        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 延迟消息插件是否启用
         */
        private Boolean delayPluginEnabled = true;

        /**
         * 延迟交换机名称
         */
        private String delayExchange = "x-delayed-exchange";

        /**
         * 默认交换机
         */
        private String defaultExchange = "basebackend.direct";

        /**
         * 默认队列前缀
         */
        private String queuePrefix = "basebackend.queue.";
    }

    @Data
    public static class Retry {
        /**
         * 是否启用重试
         */
        private Boolean enabled = true;

        /**
         * 最大重试次数
         */
        private Integer maxAttempts = 3;

        /**
         * 重试间隔（毫秒）
         */
        private Long initialInterval = 1000L;

        /**
         * 重试间隔倍数
         */
        private Double multiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        private Long maxInterval = 10000L;
    }

    @Data
    public static class DeadLetter {
        /**
         * 是否启用死信队列
         */
        private Boolean enabled = true;

        /**
         * 死信交换机
         */
        private String exchange = "basebackend.dlx";

        /**
         * 死信队列
         */
        private String queue = "basebackend.dlq";

        /**
         * 死信路由键
         */
        private String routingKey = "basebackend.dlq.#";
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
