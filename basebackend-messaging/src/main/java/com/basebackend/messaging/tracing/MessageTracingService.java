package com.basebackend.messaging.tracing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息追踪服务
 * <p>
 * 提供消息的分布式追踪能力，记录消息的完整生命周期。
 * 可与Sleuth、Zipkin等分布式追踪系统集成。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTracingService {

    private final StringRedisTemplate redisTemplate;

    private static final String TRACE_KEY_PREFIX = "msg:trace:";
    private static final Duration TRACE_EXPIRE = Duration.ofDays(7);

    /**
     * 生成追踪ID
     *
     * @return 追踪ID
     */
    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成跨度ID
     *
     * @return 跨度ID
     */
    public String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    /**
     * 创建消息追踪
     *
     * @param messageId  消息ID
     * @param traceId    追踪ID
     * @param topic      消息主题
     * @param producerId 生产者ID
     * @return 消息追踪对象
     */
    public MessageTrace createTrace(String messageId, String traceId, String topic, String producerId) {
        MessageTrace trace = new MessageTrace();
        trace.setMessageId(messageId);
        trace.setTraceId(traceId != null ? traceId : generateTraceId());
        trace.setSpanId(generateSpanId());
        trace.setTopic(topic);
        trace.setProducerId(producerId);
        trace.setCreateTime(LocalDateTime.now());

        // 添加创建事件
        trace.addEvent(MessageTrace.TraceEvent.success(
                MessageTrace.EventType.MESSAGE_CREATED,
                producerId,
                0));

        saveTrace(trace);
        log.debug("Created message trace: messageId={}, traceId={}", messageId, trace.getTraceId());

        return trace;
    }

    /**
     * 记录消息发送
     *
     * @param messageId 消息ID
     * @param latencyMs 发送耗时
     * @param success   是否成功
     * @param error     错误信息
     */
    public void recordSend(String messageId, long latencyMs, boolean success, String error) {
        MessageTrace trace = getTrace(messageId);
        if (trace == null) {
            log.warn("Trace not found for messageId: {}", messageId);
            return;
        }

        MessageTrace.TraceEvent event;
        if (success) {
            event = MessageTrace.TraceEvent.success(
                    MessageTrace.EventType.MESSAGE_SENT,
                    trace.getProducerId(),
                    latencyMs);
        } else {
            event = MessageTrace.TraceEvent.failure(
                    MessageTrace.EventType.MESSAGE_SENT,
                    trace.getProducerId(),
                    error);
        }

        trace.addEvent(event);
        saveTrace(trace);
    }

    /**
     * 记录消息消费
     *
     * @param messageId     消息ID
     * @param consumerGroup 消费者组
     * @param latencyMs     消费耗时
     * @param success       是否成功
     * @param error         错误信息
     */
    public void recordConsume(String messageId, String consumerGroup, long latencyMs,
            boolean success, String error) {
        MessageTrace trace = getTrace(messageId);
        if (trace == null) {
            log.warn("Trace not found for messageId: {}", messageId);
            return;
        }

        trace.setConsumerGroup(consumerGroup);

        MessageTrace.TraceEvent event;
        if (success) {
            event = MessageTrace.TraceEvent.success(
                    MessageTrace.EventType.CONSUME_COMPLETED,
                    consumerGroup,
                    latencyMs);
        } else {
            event = MessageTrace.TraceEvent.failure(
                    MessageTrace.EventType.CONSUME_FAILED,
                    consumerGroup,
                    error);
        }

        trace.addEvent(event);

        // 计算总耗时
        if (success && trace.getCreateTime() != null) {
            trace.setTotalLatencyMs(
                    Duration.between(trace.getCreateTime(), LocalDateTime.now()).toMillis());
        }

        saveTrace(trace);
    }

    /**
     * 记录消息重试
     *
     * @param messageId     消息ID
     * @param retryCount    重试次数
     * @param consumerGroup 消费者组
     */
    public void recordRetry(String messageId, int retryCount, String consumerGroup) {
        MessageTrace trace = getTrace(messageId);
        if (trace == null) {
            return;
        }

        MessageTrace.TraceEvent event = MessageTrace.TraceEvent.success(
                MessageTrace.EventType.MESSAGE_RETRY,
                consumerGroup,
                0);
        event.setDetails("Retry count: " + retryCount);

        trace.addEvent(event);
        saveTrace(trace);
    }

    /**
     * 记录进入死信队列
     *
     * @param messageId 消息ID
     * @param reason    进入死信原因
     */
    public void recordDeadLetter(String messageId, String reason) {
        MessageTrace trace = getTrace(messageId);
        if (trace == null) {
            return;
        }

        MessageTrace.TraceEvent event = MessageTrace.TraceEvent.failure(
                MessageTrace.EventType.TO_DEAD_LETTER,
                trace.getConsumerGroup(),
                reason);

        trace.addEvent(event);
        saveTrace(trace);
    }

    /**
     * 获取消息追踪
     *
     * @param messageId 消息ID
     * @return 追踪信息
     */
    public MessageTrace getTrace(String messageId) {
        try {
            String key = TRACE_KEY_PREFIX + messageId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return JSON.parseObject(json, MessageTrace.class);
            }
        } catch (Exception e) {
            log.error("Failed to get trace: messageId={}", messageId, e);
        }
        return null;
    }

    /**
     * 保存消息追踪
     */
    private void saveTrace(MessageTrace trace) {
        try {
            String key = TRACE_KEY_PREFIX + trace.getMessageId();
            String json = JSON.toJSONString(trace);
            redisTemplate.opsForValue().set(key, json, TRACE_EXPIRE);
        } catch (Exception e) {
            log.error("Failed to save trace: messageId={}", trace.getMessageId(), e);
        }
    }
}
