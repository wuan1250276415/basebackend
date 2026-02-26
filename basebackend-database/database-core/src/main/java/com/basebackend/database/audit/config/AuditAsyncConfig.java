package com.basebackend.database.audit.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 审计日志异步配置
 * <p>
 * 使用虚拟线程替代传统线程池，适用于 I/O 密集型的审计日志写入。
 * </p>
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "database.enhanced.audit",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AuditAsyncConfig {

    /**
     * 审计日志异步执行器（虚拟线程）
     */
    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("audit-log-");
        executor.setVirtualThreads(true);

        log.info("Initialized audit log executor with virtual threads");

        return executor;
    }
}
