package com.basebackend.messaging.handler;

import com.basebackend.messaging.consumer.MessageConsumer;
import com.basebackend.messaging.exception.MessageConsumeException;
import com.basebackend.messaging.idempotency.IdempotencyService;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.transaction.TransactionalMessageService;
import com.basebackend.messaging.model.MessageStatus;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.stereotype.Component;

/**
 * 消息处理器
 * 封装消息消费的通用逻辑（幂等性、重试、死信等）
 */
@Slf4j
@Component
public class MessageHandler {

    private final IdempotencyService idempotencyService;
    private final TransactionalMessageService transactionalMessageService;

    public MessageHandler(IdempotencyService idempotencyService,
                          TransactionalMessageService transactionalMessageService) {
        this.idempotencyService = idempotencyService;
        this.transactionalMessageService = transactionalMessageService;
    }

    /**
     * 处理消息
     *
     * @param message       消息对象
     * @param consumer      消费者函数
     * @param channel       RabbitMQ通道
     * @param deliveryTag   投递标签
     * @param <T>           消息体类型
     */
    public <T> void handle(Message<T> message,
                           MessageConsumer<T> consumer,
                           Channel channel,
                           long deliveryTag) {
        String messageId = message.getMessageId();

        try {
            // 幂等性检查
            if (idempotencyService.isDuplicate(messageId)) {
                log.warn("Duplicate message detected, skip processing: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 尝试获取处理锁（防止并发处理）
            if (!idempotencyService.tryLock(messageId)) {
                log.warn("Message is being processed by another consumer: messageId={}", messageId);
                channel.basicNack(deliveryTag, false, true); // 重新入队
                return;
            }

            try {
                // 更新状态为消费中
                if (message.getTransactional()) {
                    transactionalMessageService.updateStatus(messageId, MessageStatus.CONSUMING);
                }

                // 执行消费逻辑
                consumer.consume(message);

                // 标记为已处理
                idempotencyService.markAsProcessed(messageId);

                // 更新状态为已消费
                if (message.getTransactional()) {
                    transactionalMessageService.updateStatus(messageId, MessageStatus.CONSUMED);
                }

                // 确认消息
                channel.basicAck(deliveryTag, false);

                log.info("Message consumed successfully: messageId={}", messageId);

            } finally {
                // 释放处理锁
                idempotencyService.unlock(messageId);
            }

        } catch (Exception e) {
            log.error("Failed to consume message: messageId={}", messageId, e);

            try {
                // 增加重试次数
                message.setRetryCount(message.getRetryCount() + 1);

                if (message.getTransactional()) {
                    transactionalMessageService.incrementRetryCount(messageId);
                }

                // 判断是否超过最大重试次数
                if (message.getRetryCount() >= message.getMaxRetries()) {
                    log.error("Message retry count exceeded, send to dead letter queue: messageId={}", messageId);

                    // 更新状态为死信
                    if (message.getTransactional()) {
                        transactionalMessageService.updateStatus(messageId, MessageStatus.DEAD_LETTER);
                    }

                    // 拒绝消息，不重新入队（进入死信队列）
                    channel.basicNack(deliveryTag, false, false);
                } else {
                    // 更新状态为失败
                    if (message.getTransactional()) {
                        transactionalMessageService.updateStatus(messageId, MessageStatus.FAILED);
                    }

                    // 拒绝消息，重新入队
                    channel.basicNack(deliveryTag, false, true);
                }

            } catch (Exception ex) {
                log.error("Failed to handle message error: messageId={}", messageId, ex);
            }
        }
    }
}
