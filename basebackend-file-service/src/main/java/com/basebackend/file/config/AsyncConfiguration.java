package com.basebackend.file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 *
 * 使用虚拟线程替代传统线程池，适用于 I/O 密集型的文件操作和审计任务。
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * 审计服务执行器（虚拟线程）
     *
     * <strong>设计原则</strong>：
     * - 虚拟线程自动由 JVM 调度，无需配置线程池参数
     * - 审计失败不应影响主业务
     * - 独立执行器避免影响其他异步任务
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("AUDIT-");
        executor.setVirtualThreads(true);

        log.info("审计执行器初始化完成（虚拟线程）");

        return executor;
    }

    /**
     * 通用异步执行器（虚拟线程）
     *
     * 用于其他不重要的异步任务
     */
    @Bean(name = "commonExecutor")
    public Executor commonExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ASYNC-");
        executor.setVirtualThreads(true);

        return executor;
    }
}
