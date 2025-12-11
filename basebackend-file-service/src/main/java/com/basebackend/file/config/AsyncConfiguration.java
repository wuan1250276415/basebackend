package com.basebackend.file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * 为审计服务和其他异步操作提供专用线程池
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * 审计服务专用线程池
     *
     * <strong>配置说明</strong>：
     * - corePoolSize: 2 - 最小活跃线程数
     * - maxPoolSize: 5 - 最大线程数
     * - queueCapacity: 100 - 队列容量
     * - keepAliveSeconds: 60 - 空闲线程存活时间
     * - rejectedPolicy: DISCARD_OLDEST - 队列满时丢弃最旧任务
     *
     * <strong>设计原则</strong>：
     * - 审计失败不应影响主业务，队列满时丢弃旧任务
     * - 线程数限制防止资源耗尽
     * - 独立线程池避免影响其他异步任务
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 基本配置
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("AUDIT-");

        // 拒绝策略：队列满时丢弃最旧任务（不阻塞主流程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 初始化
        executor.initialize();

        log.info("审计线程池初始化完成: core={}, max={}, queue={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    /**
     * 通用异步线程池（备用）
     *
     * 用于其他不重要的异步任务
     */
    @Bean(name = "commonExecutor")
    public Executor commonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("ASYNC-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());

        executor.initialize();
        return executor;
    }
}
