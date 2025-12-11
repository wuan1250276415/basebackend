package com.basebackend.backup.config;

import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.RedissonLockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.LocalStorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.S3StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 备份系统自动配置类
 * <p>
 * 负责装配所有核心组件，包括：
 * <ul>
 * <li>重试模板 - 用于处理临时性失败</li>
 * <li>分布式锁管理器 - 用于防止并发执行</li>
 * <li>校验服务 - 用于验证文件完整性</li>
 * </ul>
 *
 * @author BaseBackend
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ComponentScan("com.basebackend.backup")
@EnableConfigurationProperties(BackupProperties.class)
@ConditionalOnProperty(prefix = "backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupAutoConfiguration {

    private final BackupProperties backupProperties;

    /**
     * 配置重试模板
     * <p>
     * 提供指数退避重试机制，用于处理备份过程中的临时性失败
     *
     * @return 重试模板实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryTemplate retryTemplate() {
        log.info("初始化重试模板，最大重试次数: {}", backupProperties.getRetry().getMaxAttempts());
        return new RetryTemplate(backupProperties);
    }

    /**
     * 配置分布式锁管理器
     * <p>
     * 基于Redisson实现的分布式锁，确保备份任务在多实例部署时不会并发执行
     *
     * @param redissonClient Redisson客户端
     * @return 分布式锁管理器实例
     */
    @Bean
    @ConditionalOnProperty(name = "backup.distributed-lock.type", havingValue = "redisson")
    @ConditionalOnMissingBean
    public LockManager lockManager(RedissonClient redissonClient) {
        log.info("初始化Redisson分布式锁管理器");
        return new RedissonLockManager(redissonClient, backupProperties);
    }

    /**
     * 配置校验服务
     * <p>
     * 提供MD5和SHA256校验功能，用于验证备份文件的完整性
     *
     * @return 校验服务实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ChecksumService checksumService() {
        log.info("初始化校验服务，启用算法: {}", backupProperties.getChecksum().getAlgorithms());
        return new ChecksumService(backupProperties);
    }

    /**
     * 配置本地存储提供者
     * <p>
     * 默认使用本地文件系统存储备份文件，作为主要存储后端
     *
     * @return 本地存储提供者实例
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "backup.storage.local.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(StorageProvider.class)
    public StorageProvider localStorageProvider() {
        log.info("初始化本地存储提供者，基础路径: {}", backupProperties.getStorage().getLocal().getBasePath());
        return new LocalStorageProvider(backupProperties);
    }

    /**
     * 配置S3存储提供者
     * <p>
     * 当配置了S3端点时，使用S3兼容存储（支持AWS S3、MinIO、阿里云OSS等）
     *
     * @return S3存储提供者实例
     */
    @Bean("s3StorageProvider")
    @ConditionalOnProperty(name = "backup.storage.s3.enabled", havingValue = "true")
    public StorageProvider s3StorageProvider() {
        log.info("初始化S3存储提供者，端点: {}, 桶: {}",
                backupProperties.getStorage().getS3().getEndpoint(),
                backupProperties.getStorage().getS3().getBucket());
        return new S3StorageProvider(backupProperties);
    }

}
