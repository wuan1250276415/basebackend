package com.basebackend.messaging.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.messaging.entity.DeadLetterEntity;
import com.basebackend.messaging.management.dto.DeadLetterView;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DeadLetterManagementService {

    private final DeadLetterMapper deadLetterMapper;
    private final ObjectProvider<MessageProducer> messageProducerProvider;

    public DeadLetterManagementService(DeadLetterMapper deadLetterMapper,
                                       ObjectProvider<MessageProducer> messageProducerProvider) {
        this.deadLetterMapper = deadLetterMapper;
        this.messageProducerProvider = messageProducerProvider;
    }

    public PageResult<DeadLetterView> getPage(long current, long size, String status) {
        LambdaQueryWrapper<DeadLetterEntity> wrapper = new LambdaQueryWrapper<DeadLetterEntity>()
                .orderByDesc(DeadLetterEntity::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(DeadLetterEntity::getStatus, status.trim());
        }

        Page<DeadLetterEntity> page = deadLetterMapper.selectPage(new Page<>(current, size), wrapper);
        List<DeadLetterView> records = page.getRecords().stream()
                .map(this::toView)
                .toList();
        return PageResult.of(records, page.getTotal(), current, size);
    }

    public DeadLetterView getById(Long id) {
        DeadLetterEntity entity = deadLetterMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return toView(entity);
    }

    public void redeliver(Long id) {
        DeadLetterEntity entity = requirePending(id);
        MessageProducer producer = requireProducer();
        sendMessage(producer, toMessage(entity));

        entity.setStatus("REDELIVERED");
        entity.setHandledTime(LocalDateTime.now());
        entity.setHandledBy(UserContextHolder.getUserId());
        deadLetterMapper.updateById(entity);
    }

    public int batchRedeliver(List<Long> ids) {
        int successCount = 0;
        for (Long id : ids) {
            redeliver(id);
            successCount++;
        }
        return successCount;
    }

    public void discard(Long id) {
        DeadLetterEntity entity = requirePending(id);
        entity.setStatus("DISCARDED");
        entity.setHandledTime(LocalDateTime.now());
        entity.setHandledBy(UserContextHolder.getUserId());
        deadLetterMapper.updateById(entity);
    }

    private DeadLetterEntity requirePending(Long id) {
        DeadLetterEntity entity = deadLetterMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("死信不存在");
        }
        if (!"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("当前死信状态不允许处理");
        }
        return entity;
    }

    private MessageProducer requireProducer() {
        MessageProducer producer = messageProducerProvider.getIfAvailable();
        if (producer == null) {
            throw new IllegalStateException("消息生产者未启用，无法重投死信");
        }
        return producer;
    }

    private void sendMessage(MessageProducer producer, Message<Object> message) {
        if (Boolean.TRUE.equals(message.getTransactional())) {
            producer.sendTransactional(message);
            return;
        }
        if (StringUtils.hasText(message.getPartitionKey())) {
            producer.sendOrdered(message, message.getPartitionKey());
            return;
        }
        if (message.getDelayMillis() != null && message.getDelayMillis() > 0) {
            producer.sendDelay(message, message.getDelayMillis());
            return;
        }
        producer.send(message);
    }

    @SuppressWarnings("unchecked")
    private Message<Object> toMessage(DeadLetterEntity entity) {
        if (StringUtils.hasText(entity.getOriginalMessage())) {
            Message<Object> message = JsonUtils.parseObject(entity.getOriginalMessage(), Message.class);
            if (message != null && StringUtils.hasText(message.getTopic())) {
                return message;
            }
        }

        Message<Object> fallback = new Message<>();
        fallback.setMessageId(entity.getMessageId());
        fallback.setTopic(entity.getTopic());
        fallback.setRoutingKey(extractRoutingKey(entity));
        fallback.setTags(entity.getTags());
        fallback.setMessageType(entity.getMessageType());
        fallback.setPayload(parseJsonOrRaw(entity.getPayload()));
        fallback.setHeaders(parseHeaders(entity));
        fallback.setRetryCount(entity.getRetryCount());
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseHeaders(DeadLetterEntity entity) {
        if (StringUtils.hasText(entity.getHeaders())) {
            return JsonUtils.parseObject(entity.getHeaders(), Map.class);
        }

        if (StringUtils.hasText(entity.getOriginalMessage())) {
            Message<Object> message = JsonUtils.parseObject(entity.getOriginalMessage(), Message.class);
            if (message != null) {
                return message.getHeaders();
            }
        }
        return null;
    }

    private Object parseJsonOrRaw(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtils.parseObject(json, Object.class);
        } catch (Exception ignored) {
            return json;
        }
    }

    private DeadLetterView toView(DeadLetterEntity entity) {
        return DeadLetterView.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .topic(entity.getTopic())
                .routingKey(extractRoutingKey(entity))
                .tags(entity.getTags())
                .messageType(entity.getMessageType())
                .payload(entity.getPayload())
                .headers(resolveHeaders(entity))
                .originalQueue(entity.getOriginalQueue())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .errorMessage(entity.getErrorMessage())
                .originalMessage(entity.getOriginalMessage())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .handledTime(entity.getHandledTime())
                .handledBy(entity.getHandledBy())
                .build();
    }

    private String resolveHeaders(DeadLetterEntity entity) {
        if (StringUtils.hasText(entity.getHeaders())) {
            return entity.getHeaders();
        }
        if (StringUtils.hasText(entity.getOriginalMessage())) {
            Message<Object> message = JsonUtils.parseObject(entity.getOriginalMessage(), Message.class);
            if (message != null && message.getHeaders() != null) {
                return JsonUtils.toJsonString(message.getHeaders());
            }
        }
        return null;
    }

    private String extractRoutingKey(DeadLetterEntity entity) {
        if (StringUtils.hasText(entity.getOriginalMessage())) {
            Message<Object> message = JsonUtils.parseObject(entity.getOriginalMessage(), Message.class);
            if (message != null && StringUtils.hasText(message.getRoutingKey())) {
                return message.getRoutingKey();
            }
            if (message != null && StringUtils.hasText(message.getTags())) {
                return message.getTags();
            }
        }
        return entity.getTags();
    }
}
