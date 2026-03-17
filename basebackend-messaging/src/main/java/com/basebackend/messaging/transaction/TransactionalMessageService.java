package com.basebackend.messaging.transaction;

import com.basebackend.messaging.config.MessagingProperties;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.model.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.basebackend.common.util.JsonUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 事务消息服务
 * 基于本地消息表实现最终一致性。
 *
 * <p>消息超时判定时间通过 {@code messaging.transaction.timeout}（分钟）配置，默认 30 分钟。
 * 消息补偿扫描间隔通过 {@code messaging.transaction.check-interval}（秒）配置，默认 60 秒。</p>
 *
 * <p>如需在补偿时重新发送消息，请通过 {@link #setCompensationHandler} 注册回调。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "messaging.transaction", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransactionalMessageService {

    private final JdbcTemplate jdbcTemplate;
    private final MessagingProperties messagingProperties;

    /** 可选的补偿回调，由上层服务注入，用于重新发送超时消息 */
    private Consumer<Map<String, Object>> compensationHandler;

    public TransactionalMessageService(JdbcTemplate jdbcTemplate, MessagingProperties messagingProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.messagingProperties = messagingProperties;
    }

    /**
     * 注册消息补偿处理回调
     *
     * @param handler 处理超时消息的回调，入参为消息 Map（含 message_id、topic 等字段）
     */
    public void setCompensationHandler(Consumer<Map<String, Object>> handler) {
        this.compensationHandler = handler;
    }

    /**
     * 保存消息到本地消息表
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
                message.getTags(),
                JsonUtils.toJsonString(message.getPayload()),
                JsonUtils.toJsonString(message.getHeaders()),
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
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String messageId, MessageStatus status) {
        String sql = "UPDATE sys_message_log SET status = ?, update_time = ? WHERE message_id = ?";
        jdbcTemplate.update(sql, status.name(), LocalDateTime.now(), messageId);
        log.debug("Message status updated: messageId={}, status={}", messageId, status);
    }

    /**
     * 增加重试次数
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementRetryCount(String messageId) {
        String sql = "UPDATE sys_message_log SET retry_count = retry_count + 1, update_time = ? WHERE message_id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), messageId);
    }

    /**
     * 更新消息为已发送状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSentStatus(String messageId, String msgId) {
        String sql = "UPDATE sys_message_log SET status = ?, mq_message_id = ?, update_time = ? WHERE message_id = ?";
        jdbcTemplate.update(sql, MessageStatus.SENT.name(), msgId, LocalDateTime.now(), messageId);
        log.info("Message marked as sent: messageId={}, mqMessageId={}", messageId, msgId);
    }

    /**
     * 更新消息为失败状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFailedStatus(String messageId, String errorMessage) {
        String sql = "UPDATE sys_message_log SET status = ?, error_message = ?, update_time = ? WHERE message_id = ?";
        jdbcTemplate.update(sql, MessageStatus.FAILED.name(), errorMessage, LocalDateTime.now(), messageId);
        log.warn("Message marked as failed: messageId={}, error={}", messageId, errorMessage);
    }

    /**
     * 消息补偿：扫描超时未确认的消息，触发补偿回调。
     * 调度间隔由 {@code messaging.transaction.check-interval} 配置（秒），默认 60 秒。
     */
    @Scheduled(fixedDelayString = "${messaging.transaction.check-interval:60}000")
    @Transactional(rollbackFor = Exception.class)
    public void compensateTimeoutMessages() {
        long timeoutMinutes = messagingProperties.getTransaction().getTimeout();

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
                    timeoutMinutes);

            if (!timeoutMessages.isEmpty()) {
                log.warn("Found {} timeout messages to compensate (timeoutMinutes={})",
                        timeoutMessages.size(), timeoutMinutes);

                if (compensationHandler != null) {
                    timeoutMessages.forEach(compensationHandler);
                } else {
                    log.warn("No compensationHandler registered; " +
                            "call setCompensationHandler() to enable automatic retry.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to compensate timeout messages", e);
        }
    }

    /**
     * 清理过期的已完成消息（每天凌晨 2 点执行）
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
