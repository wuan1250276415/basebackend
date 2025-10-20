package com.basebackend.admin.service.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.entity.messaging.SysDeadLetter;
import com.basebackend.admin.mapper.messaging.SysDeadLetterMapper;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.messaging.model.Message;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 死信处理服务
 */
@Slf4j
@Service
public class DeadLetterService {

    private final SysDeadLetterMapper deadLetterMapper;
    private final MessageProducer messageProducer;

    public DeadLetterService(SysDeadLetterMapper deadLetterMapper, MessageProducer messageProducer) {
        this.deadLetterMapper = deadLetterMapper;
        this.messageProducer = messageProducer;
    }

    /**
     * 分页查询死信
     */
    public Page<SysDeadLetter> getDeadLetterPage(Integer page, Integer size, String status) {
        Page<SysDeadLetter> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysDeadLetter> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(SysDeadLetter::getStatus, status);
        }
        queryWrapper.orderByDesc(SysDeadLetter::getCreateTime);
        return deadLetterMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 根据ID获取死信详情
     */
    public SysDeadLetter getDeadLetterById(Long id) {
        return deadLetterMapper.selectById(id);
    }

    /**
     * 重新投递死信
     */
    @Transactional(rollbackFor = Exception.class)
    public void redeliverDeadLetter(Long id) {
        SysDeadLetter deadLetter = deadLetterMapper.selectById(id);
        if (deadLetter == null) {
            throw new RuntimeException("死信不存在");
        }

        if (!"PENDING".equals(deadLetter.getStatus())) {
            throw new RuntimeException("死信状态不是待处理，无法重投");
        }

        try {
            // 构建消息
            Map<String, Object> payload = JSON.parseObject(deadLetter.getPayload(), Map.class);
            Message<Object> message = Message.builder()
                    .messageId(deadLetter.getMessageId())
                    .topic(deadLetter.getTopic())
                    .routingKey(deadLetter.getRoutingKey())
                    .payload(payload)
                    .build();

            // 重新发送消息
            messageProducer.send(message);

            // 更新死信状态
            SysDeadLetter update = new SysDeadLetter();
            update.setId(id);
            update.setStatus("REDELIVERED");
            update.setHandledTime(LocalDateTime.now());
            deadLetterMapper.updateById(update);

            log.info("Dead letter redelivered successfully: id={}, messageId={}", id, deadLetter.getMessageId());
        } catch (Exception e) {
            log.error("Failed to redeliver dead letter: id={}", id, e);
            throw new RuntimeException("重投死信失败: " + e.getMessage());
        }
    }

    /**
     * 丢弃死信
     */
    @Transactional(rollbackFor = Exception.class)
    public void discardDeadLetter(Long id) {
        SysDeadLetter deadLetter = deadLetterMapper.selectById(id);
        if (deadLetter == null) {
            throw new RuntimeException("死信不存在");
        }

        SysDeadLetter update = new SysDeadLetter();
        update.setId(id);
        update.setStatus("DISCARDED");
        update.setHandledTime(LocalDateTime.now());
        deadLetterMapper.updateById(update);

        log.info("Dead letter discarded: id={}, messageId={}", id, deadLetter.getMessageId());
    }

    /**
     * 批量重新投递
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchRedeliver(Long[] ids) {
        for (Long id : ids) {
            try {
                redeliverDeadLetter(id);
            } catch (Exception e) {
                log.error("Failed to redeliver dead letter: id={}", id, e);
            }
        }
    }
}
