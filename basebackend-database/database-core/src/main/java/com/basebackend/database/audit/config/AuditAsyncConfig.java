package com.basebackend.database.audit.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 审计日志异步配置
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

    private final DatabaseEnhancedProperties properties;

    /**
     * 审计日志异步执行器
     */
    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        DatabaseEnhancedProperties.ThreadPoolProperties threadPool = 
            properties.getAudit().getThreadPool();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPool.getCoreSize());
        executor.setMaxPoolSize(threadPool.getMaxSize());
        executor.setQueueCapacity(threadPool.getQueueCapacity());
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("Initialized audit log executor with coreSize={}, maxSize={}, queueCapacity={}", 
                threadPool.getCoreSize(), threadPool.getMaxSize(), threadPool.getQueueCapacity());
        
        return executor;
    }
}
