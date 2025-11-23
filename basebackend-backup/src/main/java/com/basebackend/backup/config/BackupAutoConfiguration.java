package com.basebackend.backup.config;

import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.RedissonLockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.LocalStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 备份系统自动配置类
 * 负责装配所有核心组件
 */
@Slf4j
@Configuration
@EnableScheduling
@ComponentScan("com.basebackend.backup")
@EnableConfigurationProperties(BackupProperties.class)
@ConditionalOnProperty(prefix = "backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupAutoConfiguration {

    public BackupAutoConfiguration() {
        log.info("备份模块已启用");
    }

    /**
     * 配置重试模板
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }

    /**
     * 配置分布式锁管理器
     */
    @Bean
    @ConditionalOnProperty(name = "backup.distributed-lock.type", havingValue = "redisson")
    @ConditionalOnMissingBean
    public LockManager lockManager() {
        return new RedissonLockManager();
    }

    /**
     * 配置校验服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ChecksumService checksumService() {
        return new ChecksumService();
    }

    /**
     * 配置本地存储提供者
     */
    @Bean
    @ConditionalOnProperty(name = "backup.storage.local.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public StorageProvider localStorageProvider() {
        return new LocalStorageProvider();
    }
}
