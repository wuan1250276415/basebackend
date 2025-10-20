package com.basebackend.messaging.transaction;

import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.model.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 事务消息服务
 * 基于本地消息表实现最终一致性
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "messaging.transaction", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransactionalMessageService {

    private final JdbcTemplate jdbcTemplate;

    public TransactionalMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存消息到本地消息表
     *
     * @param message 消息对象
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> void saveMessage(Message<T> message) {
        String sql = """
                INSERT INTO sys_message_log (
                    message_id, topic, routing_key, tag, payload, headers,
                    send_time, delay_millis, retry_count, max_retries,
                    partition_key, status, create_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                message.getMessageId(),
                message.getTopic(),
                message.getRoutingKey(),
                message.getTag(),
                JSON.toJSONString(message.getPayload()),
                JSON.toJSONString(message.getHeaders()),
                message.getSendTime(),
                message.getDelayMillis(),
                message.getRetryCount(),
                message.getMaxRetries(),
                message.getPartitionKey(),
                MessageStatus.PENDING.name(),
                LocalDateTime.now()
        );

        log.info("Transactional message saved: messageId={}", message.getMessageId());
    }

    /**
     * 更新消息状态
     *
     * @param messageId 消息ID
     * @param status    状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String messageId, MessageStatus status) {
        String sql = "UPDATE sys_message_log SET status = ?, update_time = ? WHERE message_id = ?";
        jdbcTemplate.update(sql, status.name(), LocalDateTime.now(), messageId);
        log.debug("Message status updated: messageId={}, status={}", messageId, status);
    }

    /**
     * 增加重试次数
     *
     * @param messageId 消息ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementRetryCount(String messageId) {
        String sql = "UPDATE sys_message_log SET retry_count = retry_count + 1, update_time = ? WHERE message_id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), messageId);
    }

    /**
     * 消息补偿：扫描超时未确认的消息并重新发送
     * 每分钟执行一次
     */
    @Scheduled(fixedDelayString = "${messaging.transaction.check-interval:60}000")
    @Transactional(rollbackFor = Exception.class)
    public void compensateTimeoutMessages() {
        String sql = """
                SELECT message_id, topic, routing_key, tag, payload, headers,
                       retry_count, max_retries
                FROM sys_message_log
                WHERE status IN (?, ?)
                  AND create_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)
                  AND retry_count < max_retries
                LIMIT 100
                """;

        try {
            List<Map<String, Object>> timeoutMessages = jdbcTemplate.queryForList(sql,
                    MessageStatus.PENDING.name(),
                    MessageStatus.FAILED.name(),
                    30); // 超时时间：30分钟

            if (!timeoutMessages.isEmpty()) {
                log.warn("Found {} timeout messages to compensate", timeoutMessages.size());
                // TODO: 重新发送消息的逻辑由上层服务实现
            }
        } catch (Exception e) {
            log.error("Failed to compensate timeout messages", e);
        }
    }

    /**
     * 清理过期的已完成消息
     * 每天执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredMessages() {
        String sql = """
                DELETE FROM sys_message_log
                WHERE status IN (?, ?)
                  AND update_time < DATE_SUB(NOW(), INTERVAL 7 DAY)
                """;

        try {
            int deleted = jdbcTemplate.update(sql,
                    MessageStatus.CONSUMED.name(),
                    MessageStatus.DEAD_LETTER.name());
            log.info("Cleaned {} expired messages", deleted);
        } catch (Exception e) {
            log.error("Failed to clean expired messages", e);
        }
    }
}
