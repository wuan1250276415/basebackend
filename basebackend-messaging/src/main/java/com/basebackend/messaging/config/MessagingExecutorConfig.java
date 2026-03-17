package com.basebackend.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * 消息处理执行器配置
 * <p>
 * 使用虚拟线程替代传统线程池，适用于 I/O 密集型的消息处理场景。
 * 虚拟线程无需配置核心线程数、最大线程数等参数，由 JVM 自动调度。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MessagingExecutorConfig {

    /**
     * 消息异步发送执行器（虚拟线程）
     * <p>用于 {@code RocketMQProducer.sendAsync} / {@code sendBatchAsync} 的异步发送。</p>
     */
    @Bean("messageSenderExecutor")
    public TaskExecutor messageSenderExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("msg-sender-");
        executor.setVirtualThreads(true);
        log.info("MessageSenderExecutor initialized with virtual threads");
        return executor;
    }

    /**
     * Webhook 调用执行器（虚拟线程）
     * <p>用于 {@code WebhookInvoker.invokeAsync} 的异步 HTTP 调用。</p>
     */
    @Bean("webhookExecutor")
    public TaskExecutor webhookExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("webhook-");
        executor.setVirtualThreads(true);
        log.info("WebhookExecutor initialized with virtual threads");
        return executor;
    }
}
