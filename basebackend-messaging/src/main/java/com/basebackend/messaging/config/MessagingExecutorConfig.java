package com.basebackend.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 消息处理线程池配置
 * <p>
 * 提供专用的线程池用于消息处理，避免阻塞主线程。
 * 支持自定义核心线程数、最大线程数、队列容量等参数。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MessagingExecutorConfig {

    @Value("${messaging.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${messaging.executor.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${messaging.executor.queue-capacity:500}")
    private int queueCapacity;

    @Value("${messaging.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${messaging.executor.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    /**
     * 消息处理线程池
     * <p>
     * 用于异步处理消息消费任务。
     * </p>
     */
    @Bean("messageProcessorExecutor")
    public ThreadPoolTaskExecutor messageProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("msg-processor-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();

        log.info("MessageProcessorExecutor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * 消息发送线程池
     * <p>
     * 用于异步发送消息。
     * </p>
     */
    @Bean("messageSenderExecutor")
    public ThreadPoolTaskExecutor messageSenderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize / 2);
        executor.setMaxPoolSize(maxPoolSize / 2);
        executor.setQueueCapacity(queueCapacity / 2);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("msg-sender-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();

        log.info("MessageSenderExecutor initialized");

        return executor;
    }

    /**
     * Webhook调用线程池
     * <p>
     * 用于异步调用Webhook。
     * </p>
     */
    @Bean("webhookExecutor")
    public ThreadPoolTaskExecutor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("webhook-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();

        log.info("WebhookExecutor initialized");

        return executor;
    }

    /**
     * 带日志记录的CallerRunsPolicy
     * <p>
     * 当线程池饱和时，在调用者线程中执行任务，并记录警告日志。
     * </p>
     */
    private static class LoggingCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Task rejected, executing in caller thread. " +
                    "Pool size: {}, Active: {}, Queue size: {}",
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size());

            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
