package com.basebackend.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 消息配置属性
 * <p>
 * 包含RocketMQ、重试、死信队列、事务消息、幂等性等配置。
 * 所有配置项都经过验证，确保配置正确。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {

    /**
     * RocketMQ配置
     */
    @Valid
    @NotNull(message = "RocketMQ配置不能为空")
    private RocketMQ rocketmq = new RocketMQ();

    /**
     * 重试配置
     */
    @Valid
    @NotNull(message = "重试配置不能为空")
    private Retry retry = new Retry();

    /**
     * 死信队列配置
     */
    @Valid
    @NotNull(message = "死信队列配置不能为空")
    private DeadLetter deadLetter = new DeadLetter();

    /**
     * 事务消息配置
     */
    @Valid
    @NotNull(message = "事务消息配置不能为空")
    private Transaction transaction = new Transaction();

    /**
     * 幂等性配置
     */
    @Valid
    @NotNull(message = "幂等性配置不能为空")
    private Idempotency idempotency = new Idempotency();

    /**
     * 加密配置
     */
    @Valid
    private Encryption encryption = new Encryption();

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
        @Min(value = 60, message = "幂等性过期时间最少60秒")
        @Max(value = 86400, message = "幂等性过期时间最多24小时")
        private Long expireTime = 3600L;

        /**
         * Redis键前缀
         */
        @NotBlank(message = "幂等性键前缀不能为空")
        private String keyPrefix = "msg:idempotent:";
    }

    @Data
    public static class Encryption {
        /**
         * 是否启用消息加密
         */
        private Boolean enabled = false;

        /**
         * 加密算法（AES、RSA等）
         */
        private String algorithm = "AES";

        /**
         * 加密密钥（建议使用环境变量或密钥管理服务）
         */
        private String secretKey;

        /**
         * 加密模式
         */
        private String mode = "GCM";

        /**
         * 需要加密的Topic列表（为空则加密所有）
         */
        private java.util.List<String> encryptTopics = new java.util.ArrayList<>();
    }
}
