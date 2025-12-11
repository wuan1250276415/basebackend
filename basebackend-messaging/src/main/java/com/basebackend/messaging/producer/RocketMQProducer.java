package com.basebackend.messaging.producer;

import com.alibaba.fastjson2.JSON;
import com.basebackend.messaging.config.MessagingProperties;
import com.basebackend.messaging.constants.RocketMQConstants;
import com.basebackend.messaging.exception.MessageSendException;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.transaction.TransactionalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RocketMQ 消息生产者
 * <p>
 * 实现同步、异步、批量、延迟、事务、顺序等多种发送方式。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMQProducer implements MessageProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final MessagingProperties messagingProperties;
    private final TransactionalMessageService transactionalMessageService;

    /** 异步发送线程池 */
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread t = new Thread(r, "msg-async-sender");
                t.setDaemon(true);
                return t;
            });

    @Override
    public <T> String send(Message<T> message) {
        log.info("发送消息: topic={}, tag={}, messageId={}",
                message.getTopic(), message.getTags(), message.getMessageId());

        try {
            String destination = buildDestination(message.getTopic(), message.getTags());
            String payload = JSON.toJSONString(message);

            org.springframework.messaging.Message<String> springMessage = MessageBuilder.withPayload(payload)
                    .setHeader("messageId", message.getMessageId())
                    .setHeader("messageType", message.getMessageType())
                    .build();

            SendResult sendResult = rocketMQTemplate.syncSend(destination, springMessage);

            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                throw new MessageSendException("消息发送失败: " + sendResult.getSendStatus());
            }

            log.info("消息发送成功: messageId={}, msgId={}, queueId={}",
                    message.getMessageId(), sendResult.getMsgId(), sendResult.getMessageQueue().getQueueId());

            return sendResult.getMsgId();

        } catch (Exception e) {
            log.error("消息发送失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            throw new MessageSendException("消息发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> CompletableFuture<String> sendAsync(Message<T> message) {
        log.debug("异步发送消息: topic={}, tag={}, messageId={}",
                message.getTopic(), message.getTags(), message.getMessageId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(message);
            } catch (Exception e) {
                log.error("异步消息发送失败: messageId={}, error={}", message.getMessageId(), e.getMessage());
                throw new MessageSendException("异步消息发送失败: " + e.getMessage(), e);
            }
        }, asyncExecutor);
    }

    @Override
    public <T> List<String> sendBatch(List<Message<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("批量发送消息: count={}", messages.size());

        List<String> results = new ArrayList<>(messages.size());
        List<String> failedIds = new ArrayList<>();

        for (Message<T> message : messages) {
            try {
                String msgId = send(message);
                results.add(msgId);
            } catch (Exception e) {
                log.error("批量发送中单条消息失败: messageId={}, error={}",
                        message.getMessageId(), e.getMessage());
                failedIds.add(message.getMessageId());
                results.add(null);
            }
        }

        if (!failedIds.isEmpty()) {
            log.warn("批量发送完成，部分失败: total={}, success={}, failed={}",
                    messages.size(), messages.size() - failedIds.size(), failedIds.size());
        } else {
            log.info("批量发送完成: total={}", messages.size());
        }

        return results;
    }

    @Override
    public <T> CompletableFuture<List<String>> sendBatchAsync(List<Message<T>> messages) {
        log.debug("异步批量发送消息: count={}", messages != null ? messages.size() : 0);

        return CompletableFuture.supplyAsync(() -> sendBatch(messages), asyncExecutor);
    }

    @Override
    public <T> String sendDelay(Message<T> message, long delayMillis) {
        log.info("发送延迟消息: topic={}, tag={}, messageId={}, delay={}ms",
                message.getTopic(), message.getTags(), message.getMessageId(), delayMillis);

        try {
            String destination = buildDestination(message.getTopic(), message.getTags());
            String payload = JSON.toJSONString(message);

            // 转换延迟时间为RocketMQ延迟级别
            int delayLevel = RocketMQConstants.getDelayLevel(delayMillis);

            org.springframework.messaging.Message<String> springMessage = MessageBuilder.withPayload(payload)
                    .setHeader("messageId", message.getMessageId())
                    .setHeader("messageType", message.getMessageType())
                    .build();

            SendResult sendResult = rocketMQTemplate.syncSend(destination, springMessage, 3000, delayLevel);

            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                throw new MessageSendException("延迟消息发送失败: " + sendResult.getSendStatus());
            }

            log.info("延迟消息发送成功: messageId={}, msgId={}, delayLevel={}",
                    message.getMessageId(), sendResult.getMsgId(), delayLevel);

            return sendResult.getMsgId();

        } catch (Exception e) {
            log.error("延迟消息发送失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            throw new MessageSendException("延迟消息发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> String sendTransactional(Message<T> message) {
        log.info("发送事务消息: topic={}, tag={}, messageId={}",
                message.getTopic(), message.getTags(), message.getMessageId());

        try {
            // 保留原有的本地消息表方案
            // 1. 先保存到本地消息表
            transactionalMessageService.saveMessage(message);

            // 2. 发送到 RocketMQ
            String msgId = send(message);

            // 3. 更新消息状态为已发送
            transactionalMessageService.updateSentStatus(message.getMessageId(), msgId);

            log.info("事务消息发送成功: messageId={}, msgId={}", message.getMessageId(), msgId);

            return msgId;

        } catch (Exception e) {
            log.error("事务消息发送失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            // 标记消息为失败，等待补偿
            transactionalMessageService.updateFailedStatus(message.getMessageId(), e.getMessage());
            throw new MessageSendException("事务消息发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> String sendOrdered(Message<T> message, String partitionKey) {
        log.info("发送顺序消息: topic={}, tag={}, messageId={}, partitionKey={}",
                message.getTopic(), message.getTags(), message.getMessageId(), partitionKey);

        try {
            String destination = buildDestination(message.getTopic(), message.getTags());
            String payload = JSON.toJSONString(message);

            org.springframework.messaging.Message<String> springMessage = MessageBuilder.withPayload(payload)
                    .setHeader("messageId", message.getMessageId())
                    .setHeader("messageType", message.getMessageType())
                    .build();

            // 使用 partitionKey 作为 hashKey，保证同一 key 的消息发送到同一队列
            SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, springMessage, partitionKey);

            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                throw new MessageSendException("顺序消息发送失败: " + sendResult.getSendStatus());
            }

            log.info("顺序消息发送成功: messageId={}, msgId={}, queueId={}",
                    message.getMessageId(), sendResult.getMsgId(), sendResult.getMessageQueue().getQueueId());

            return sendResult.getMsgId();

        } catch (Exception e) {
            log.error("顺序消息发送失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            throw new MessageSendException("顺序消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建消息目的地 (topic:tag)
     */
    private String buildDestination(String topic, String tag) {
        if (topic == null || topic.isEmpty()) {
            topic = messagingProperties.getRocketmq().getDefaultTopic();
        }

        if (tag != null && !tag.isEmpty()) {
            return topic + ":" + tag;
        }

        return topic;
    }
}
