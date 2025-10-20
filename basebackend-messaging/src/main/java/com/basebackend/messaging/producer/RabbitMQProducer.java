package com.basebackend.messaging.producer;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.basebackend.messaging.config.MessagingProperties;
import com.basebackend.messaging.exception.MessageSendException;
import com.basebackend.messaging.idempotency.IdempotencyService;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.transaction.TransactionalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * RabbitMQ消息生产者实现
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "messaging.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQProducer implements MessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;
    private final IdempotencyService idempotencyService;
    private final TransactionalMessageService transactionalMessageService;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate,
                            MessagingProperties properties,
                            IdempotencyService idempotencyService,
                            TransactionalMessageService transactionalMessageService) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.idempotencyService = idempotencyService;
        this.transactionalMessageService = transactionalMessageService;
    }

    @Override
    public <T> String send(Message<T> message) {
        prepareMessage(message);

        try {
            String exchange = message.getTopic() != null ? message.getTopic() : properties.getRabbitmq().getDefaultExchange();
            String routingKey = message.getRoutingKey() != null ? message.getRoutingKey() : "";

            rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
                MessageProperties props = msg.getMessageProperties();
                props.setMessageId(message.getMessageId());
                if (message.getHeaders() != null) {
                    message.getHeaders().forEach((key, value) -> props.setHeader(key, value));
                }
                return msg;
            });

            log.info("Message sent successfully: messageId={}, topic={}, routingKey={}",
                    message.getMessageId(), exchange, routingKey);

            return message.getMessageId();
        } catch (Exception e) {
            log.error("Failed to send message: messageId={}", message.getMessageId(), e);
            throw new MessageSendException("Failed to send message", e);
        }
    }

    @Override
    public <T> String sendDelay(Message<T> message, long delayMillis) {
        if (!properties.getRabbitmq().getDelayPluginEnabled()) {
            throw new MessageSendException("Delay message plugin is not enabled");
        }

        prepareMessage(message);
        message.setDelayMillis(delayMillis);

        try {
            String exchange = properties.getRabbitmq().getDelayExchange();
            String routingKey = message.getRoutingKey() != null ? message.getRoutingKey() : "";

            rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
                MessageProperties props = msg.getMessageProperties();
                props.setMessageId(message.getMessageId());
                props.setDelay((int) delayMillis);
                if (message.getHeaders() != null) {
                    message.getHeaders().forEach((key, value) -> props.setHeader(key, value));
                }
                return msg;
            });

            log.info("Delay message sent successfully: messageId={}, delayMillis={}",
                    message.getMessageId(), delayMillis);

            return message.getMessageId();
        } catch (Exception e) {
            log.error("Failed to send delay message: messageId={}", message.getMessageId(), e);
            throw new MessageSendException("Failed to send delay message", e);
        }
    }

    @Override
    public <T> String sendTransactional(Message<T> message) {
        if (!properties.getTransaction().getEnabled()) {
            throw new MessageSendException("Transactional message is not enabled");
        }

        prepareMessage(message);
        message.setTransactional(true);

        // 保存到本地消息表
        transactionalMessageService.saveMessage(message);

        // 发送消息
        return send(message);
    }

    @Override
    public <T> String sendOrdered(Message<T> message, String partitionKey) {
        prepareMessage(message);
        message.setPartitionKey(partitionKey);

        // 为顺序消息设置特殊的路由键，确保相同partitionKey的消息路由到同一队列
        String routingKey = message.getTopic() + ".ordered." + Math.abs(partitionKey.hashCode() % 10);
        message.setRoutingKey(routingKey);

        return send(message);
    }

    /**
     * 准备消息（设置默认值）
     */
    private <T> void prepareMessage(Message<T> message) {
        if (message.getMessageId() == null) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }
        if (message.getSendTime() == null) {
            message.setSendTime(LocalDateTime.now());
        }
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }
        if (message.getMaxRetries() == null) {
            message.setMaxRetries(properties.getRetry().getMaxAttempts());
        }
        if (message.getTransactional() == null) {
            message.setTransactional(false);
        }
    }
}
