package com.basebackend.messaging.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.messaging.entity.DeadLetterEntity;
import com.basebackend.messaging.entity.MessageLogEntity;
import com.basebackend.messaging.management.dto.MessagingStatisticsResponse;
import com.basebackend.messaging.management.dto.QueueMonitorResponse;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import com.basebackend.messaging.mapper.MessageLogMapper;
import com.basebackend.messaging.model.MessageStatus;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.ListableBeanFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class MessagingMonitorService {

    private final MessageLogMapper messageLogMapper;
    private final DeadLetterMapper deadLetterMapper;
    private final ListableBeanFactory beanFactory;

    public MessagingMonitorService(MessageLogMapper messageLogMapper,
                                   DeadLetterMapper deadLetterMapper,
                                   ListableBeanFactory beanFactory) {
        this.messageLogMapper = messageLogMapper;
        this.deadLetterMapper = deadLetterMapper;
        this.beanFactory = beanFactory;
    }

    public MessagingStatisticsResponse getStatistics() {
        long total = countMessages();
        long pending = countByStatus(MessageStatus.PENDING.name());
        long sent = countByStatus(MessageStatus.SENT.name());
        long consumed = countByStatus(MessageStatus.CONSUMED.name());
        long failed = countByStatus(MessageStatus.FAILED.name());
        long deadLetter = deadLetterMapper.selectCount(new LambdaQueryWrapper<>());

        String successRate = total == 0
                ? "0.00%"
                : BigDecimal.valueOf(consumed * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP) + "%";

        return new MessagingStatisticsResponse(total, pending, sent, consumed, failed, deadLetter, successRate);
    }

    public QueueMonitorResponse getQueueMonitor() {
        long totalMessages = countMessages();
        long readyMessages = countByStatus(MessageStatus.PENDING.name());
        long unackedMessages = countByStatuses(MessageStatus.SENT.name(),
                MessageStatus.DELIVERED.name(),
                MessageStatus.CONSUMING.name());
        long consumerCount = beanFactory.getBeansOfType(RocketMQListener.class).size();
        long queueCount = messageLogMapper.selectObjs(new LambdaQueryWrapper<MessageLogEntity>()
                .select(MessageLogEntity::getTopic)
                .groupBy(MessageLogEntity::getTopic)).size();

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentMessages = messageLogMapper.selectCount(new LambdaQueryWrapper<MessageLogEntity>()
                .ge(MessageLogEntity::getCreateTime, oneMinuteAgo));
        long recentConsumed = messageLogMapper.selectCount(new LambdaQueryWrapper<MessageLogEntity>()
                .eq(MessageLogEntity::getStatus, MessageStatus.CONSUMED.name())
                .ge(MessageLogEntity::getUpdateTime, oneMinuteAgo));

        double messageRate = scaleToTwoDecimals(recentMessages / 60.0);
        double ackRate = scaleToTwoDecimals(recentConsumed / 60.0);

        return new QueueMonitorResponse(queueCount, totalMessages, readyMessages,
                unackedMessages, consumerCount, messageRate, ackRate);
    }

    private long countMessages() {
        return messageLogMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countByStatus(String status) {
        return messageLogMapper.selectCount(new LambdaQueryWrapper<MessageLogEntity>()
                .eq(MessageLogEntity::getStatus, status));
    }

    private long countByStatuses(String... statuses) {
        return messageLogMapper.selectCount(new LambdaQueryWrapper<MessageLogEntity>()
                .in(MessageLogEntity::getStatus, (Object[]) statuses));
    }

    private double scaleToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
