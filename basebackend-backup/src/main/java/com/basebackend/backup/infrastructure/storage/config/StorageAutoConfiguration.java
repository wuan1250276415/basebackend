package com.basebackend.backup.infrastructure.storage.config;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.LocalStorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.S3StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 存储提供者自动配置
 * 根据配置自动选择本地存储或S3云存储
 */
@Slf4j
@Configuration
public class StorageAutoConfiguration {

    /**
     * 配置S3存储提供者
     * 仅当启用S3且禁用S3时才生效
     */
    @Bean
    @ConditionalOnProperty(name = "backup.storage.s3.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public StorageProvider s3StorageProvider(BackupProperties backupProperties) {
        log.info("配置S3云存储提供者");
        return new S3StorageProvider();
    }

    /**
     * 配置本地存储提供者
     * 仅当禁用S3或S3未启用时生效
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "backup.storage.s3.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean
    public StorageProvider localStorageProvider(BackupProperties backupProperties) {
        log.info("配置本地存储提供者");
        return new LocalStorageProvider();
    }
}
