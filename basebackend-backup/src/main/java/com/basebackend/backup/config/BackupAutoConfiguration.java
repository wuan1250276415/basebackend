package com.basebackend.backup.config;

import com.basebackend.backup.infrastructure.notification.BackupNotificationSender;
import com.basebackend.backup.infrastructure.notification.impl.CompositeNotificationService;
import com.basebackend.backup.infrastructure.notification.impl.DingTalkNotificationSender;
import com.basebackend.backup.infrastructure.notification.impl.EmailNotificationSender;
import com.basebackend.backup.infrastructure.notification.impl.SlackNotificationSender;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.LocalStorageProvider;
import com.basebackend.backup.infrastructure.storage.impl.S3StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * 备份系统自动配置类
 * <p>
 * 负责装配所有核心组件，包括：
 * <ul>
 * <li>重试模板 - 用于处理临时性失败</li>
 * <li>校验服务 - 用于验证文件完整性</li>
 * <li>存储提供者 - 本地/S3</li>
 * <li>通知发送器 - 钉钉/邮件/Slack（按需启用）</li>
 * </ul>
 * <p>
 * 注意：使用 @Component 的类由组件扫描自动注册，不在此处列出。
 * 通知发送器等非 @Component 类通过 @Bean 方法按需注册。
 *
 * @author BaseBackend
 */
@Slf4j
@AutoConfiguration
@EnableScheduling
@RequiredArgsConstructor
@EnableConfigurationProperties(BackupProperties.class)
@ConditionalOnProperty(prefix = "backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupAutoConfiguration {

    private final BackupProperties backupProperties;

    /**
     * 配置重试模板
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryTemplate retryTemplate() {
        log.info("初始化重试模板，最大重试次数: {}", backupProperties.getRetry().getMaxAttempts());
        return new RetryTemplate(backupProperties);
    }

    /**
     * 配置校验服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ChecksumService checksumService() {
        log.info("初始化校验服务，启用算法: {}", backupProperties.getChecksum().getAlgorithms());
        return new ChecksumService(backupProperties);
    }

    /**
     * 配置本地存储提供者（默认启用）
     */
    @Bean("backupLocalStorageProvider")
    @Primary
    @ConditionalOnProperty(name = "backup.storage.local.enabled", havingValue = "true", matchIfMissing = true)
    public StorageProvider localStorageProvider() {
        log.info("初始化本地存储提供者，基础路径: {}", backupProperties.getStorage().getLocal().getBasePath());
        return new LocalStorageProvider(backupProperties);
    }

    /**
     * 配置S3存储提供者
     */
    @Bean("s3StorageProvider")
    @ConditionalOnProperty(name = "backup.storage.s3.enabled", havingValue = "true")
    public StorageProvider s3StorageProvider() {
        log.info("初始化S3存储提供者，端点: {}, 桶: {}",
                backupProperties.getStorage().getS3().getEndpoint(),
                backupProperties.getStorage().getS3().getBucket());
        return new S3StorageProvider(backupProperties);
    }

    // ========== 通知发送器（按需注册） ==========

    @Bean
    @ConditionalOnProperty(name = "backup.notify.dingtalk.webhook-url")
    public DingTalkNotificationSender dingTalkNotificationSender() {
        log.info("初始化钉钉通知发送器");
        return new DingTalkNotificationSender(backupProperties.getNotify().getDingTalk());
    }

    @Bean
    @ConditionalOnProperty(name = "backup.notify.email.smtp-host")
    public EmailNotificationSender emailNotificationSender() {
        log.info("初始化邮件通知发送器");
        return new EmailNotificationSender(backupProperties.getNotify().getEmail());
    }

    @Bean
    @ConditionalOnProperty(name = "backup.notify.slack.webhook-url")
    public SlackNotificationSender slackNotificationSender() {
        log.info("初始化Slack通知发送器");
        return new SlackNotificationSender(backupProperties.getNotify().getSlack());
    }

    @Bean
    @ConditionalOnMissingBean
    public CompositeNotificationService compositeNotificationService(
            List<BackupNotificationSender> senders) {
        return new CompositeNotificationService(senders);
    }
}
