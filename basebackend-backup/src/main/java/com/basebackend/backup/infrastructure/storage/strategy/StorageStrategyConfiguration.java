package com.basebackend.backup.infrastructure.storage.strategy;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储策略自动配置
 */
@Slf4j
@Configuration
public class StorageStrategyConfiguration {

    /**
     * 配置存储策略执行器
     */
    @Bean
    @ConditionalOnProperty(name = "backup.storage.multi-replica.enabled", havingValue = "true")
    public StorageStrategyExecutor storageStrategyExecutor(BackupProperties backupProperties,
                                                           LockManager lockManager,
                                                           RetryTemplate retryTemplate) {
        log.info("配置多副本存储策略执行器");
        return new StorageStrategyExecutor(backupProperties, lockManager, retryTemplate);
    }
}
