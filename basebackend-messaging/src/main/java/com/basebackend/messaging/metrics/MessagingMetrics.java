package com.basebackend.messaging.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 消息服务监控指标
 * <p>
 * 提供消息发送、消费、重试等操作的监控指标，
 * 支持与Prometheus/Grafana集成。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class MessagingMetrics {

    private final MeterRegistry registry;

    // ========== 指标名称常量 ==========
    private static final String METRIC_PREFIX = "messaging";

    // 发送指标
    private static final String SEND_TOTAL = METRIC_PREFIX + ".send.total";
    private static final String SEND_SUCCESS = METRIC_PREFIX + ".send.success";
    private static final String SEND_FAILURE = METRIC_PREFIX + ".send.failure";
    private static final String SEND_LATENCY = METRIC_PREFIX + ".send.latency";

    // 消费指标
    private static final String CONSUME_TOTAL = METRIC_PREFIX + ".consume.total";
    private static final String CONSUME_SUCCESS = METRIC_PREFIX + ".consume.success";
    private static final String CONSUME_FAILURE = METRIC_PREFIX + ".consume.failure";
    private static final String CONSUME_LATENCY = METRIC_PREFIX + ".consume.latency";

    // 重试指标
    private static final String RETRY_TOTAL = METRIC_PREFIX + ".retry.total";
    private static final String DEAD_LETTER_TOTAL = METRIC_PREFIX + ".deadletter.total";

    // 幂等性指标
    private static final String IDEMPOTENT_HIT = METRIC_PREFIX + ".idempotent.hit";
    private static final String IDEMPOTENT_MISS = METRIC_PREFIX + ".idempotent.miss";

    // 事务消息指标
    private static final String TRANSACTION_PENDING = METRIC_PREFIX + ".transaction.pending";
    private static final String TRANSACTION_COMPENSATE = METRIC_PREFIX + ".transaction.compensate";

    public MessagingMetrics(MeterRegistry registry) {
        this.registry = registry;
        log.info("MessagingMetrics initialized");
    }

    // ========== 发送指标 ==========

    /**
     * 记录消息发送成功
     *
     * @param topic 消息主题
     */
    public void recordSendSuccess(String topic) {
        Counter.builder(SEND_TOTAL)
                .tag("topic", topic)
                .tag("status", "success")
                .register(registry)
                .increment();

        Counter.builder(SEND_SUCCESS)
                .tag("topic", topic)
                .register(registry)
                .increment();
    }

    /**
     * 记录消息发送失败
     *
     * @param topic     消息主题
     * @param errorType 错误类型
     */
    public void recordSendFailure(String topic, String errorType) {
        Counter.builder(SEND_TOTAL)
                .tag("topic", topic)
                .tag("status", "failure")
                .register(registry)
                .increment();

        Counter.builder(SEND_FAILURE)
                .tag("topic", topic)
                .tag("error", errorType)
                .register(registry)
                .increment();
    }

    /**
     * 记录消息发送耗时
     *
     * @param topic     消息主题
     * @param latencyMs 耗时（毫秒）
     */
    public void recordSendLatency(String topic, long latencyMs) {
        Timer.builder(SEND_LATENCY)
                .tag("topic", topic)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录消息发送（带计时器）
     *
     * @param topic 消息主题
     * @return 计时器Sample
     */
    public Timer.Sample startSendTimer(String topic) {
        return Timer.start(registry);
    }

    /**
     * 停止发送计时器并记录
     *
     * @param sample  计时器Sample
     * @param topic   消息主题
     * @param success 是否成功
     */
    public void stopSendTimer(Timer.Sample sample, String topic, boolean success) {
        Timer timer = Timer.builder(SEND_LATENCY)
                .tag("topic", topic)
                .tag("status", success ? "success" : "failure")
                .register(registry);
        sample.stop(timer);

        if (success) {
            recordSendSuccess(topic);
        } else {
            recordSendFailure(topic, "unknown");
        }
    }

    // ========== 消费指标 ==========

    /**
     * 记录消息消费成功
     *
     * @param topic         消息主题
     * @param consumerGroup 消费者组
     */
    public void recordConsumeSuccess(String topic, String consumerGroup) {
        Counter.builder(CONSUME_TOTAL)
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .tag("status", "success")
                .register(registry)
                .increment();

        Counter.builder(CONSUME_SUCCESS)
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .register(registry)
                .increment();
    }

    /**
     * 记录消息消费失败
     *
     * @param topic         消息主题
     * @param consumerGroup 消费者组
     * @param errorType     错误类型
     */
    public void recordConsumeFailure(String topic, String consumerGroup, String errorType) {
        Counter.builder(CONSUME_TOTAL)
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .tag("status", "failure")
                .register(registry)
                .increment();

        Counter.builder(CONSUME_FAILURE)
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .tag("error", errorType)
                .register(registry)
                .increment();
    }

    /**
     * 记录消息消费耗时
     *
     * @param topic         消息主题
     * @param consumerGroup 消费者组
     * @param latencyMs     耗时（毫秒）
     */
    public void recordConsumeLatency(String topic, String consumerGroup, long latencyMs) {
        Timer.builder(CONSUME_LATENCY)
                .tag("topic", topic)
                .tag("consumer_group", consumerGroup)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    // ========== 重试和死信指标 ==========

    /**
     * 记录消息重试
     *
     * @param topic      消息主题
     * @param retryCount 重试次数
     */
    public void recordRetry(String topic, int retryCount) {
        Counter.builder(RETRY_TOTAL)
                .tag("topic", topic)
                .tag("retry_count", String.valueOf(retryCount))
                .register(registry)
                .increment();
    }

    /**
     * 记录死信消息
     *
     * @param topic  原始消息主题
     * @param reason 进入死信原因
     */
    public void recordDeadLetter(String topic, String reason) {
        Counter.builder(DEAD_LETTER_TOTAL)
                .tag("topic", topic)
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    // ========== 幂等性指标 ==========

    /**
     * 记录幂等性命中（重复消息被过滤）
     *
     * @param topic 消息主题
     */
    public void recordIdempotentHit(String topic) {
        Counter.builder(IDEMPOTENT_HIT)
                .tag("topic", topic)
                .register(registry)
                .increment();
    }

    /**
     * 记录幂等性未命中（新消息）
     *
     * @param topic 消息主题
     */
    public void recordIdempotentMiss(String topic) {
        Counter.builder(IDEMPOTENT_MISS)
                .tag("topic", topic)
                .register(registry)
                .increment();
    }

    // ========== 事务消息指标 ==========

    /**
     * 设置待处理事务消息数量
     *
     * @param count 待处理数量
     */
    public void setPendingTransactions(long count) {
        registry.gauge(TRANSACTION_PENDING, count);
    }

    /**
     * 记录事务消息补偿
     *
     * @param messageId 消息ID
     */
    public void recordTransactionCompensate(String messageId) {
        Counter.builder(TRANSACTION_COMPENSATE)
                .register(registry)
                .increment();
    }

    // ========== 便捷方法 ==========

    /**
     * 测量操作耗时
     *
     * @param metricName 指标名称
     * @param tags       标签
     * @param runnable   要执行的操作
     */
    public void measureTime(String metricName, String[] tags, Runnable runnable) {
        Timer.Sample sample = Timer.start(registry);
        try {
            runnable.run();
        } finally {
            Timer.Builder builder = Timer.builder(metricName);
            for (int i = 0; i < tags.length; i += 2) {
                if (i + 1 < tags.length) {
                    builder.tag(tags[i], tags[i + 1]);
                }
            }
            sample.stop(builder.register(registry));
        }
    }
}
