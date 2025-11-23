package com.basebackend.logging.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

/**
 * 审计系统配置属性
 *
 * 从 application.yml 中读取审计系统相关配置。
 * 支持自动配置和配置验证。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Data
@Validated
@ConfigurationProperties(prefix = "basebackend.logging.audit")
public class AuditProperties {

    /**
     * 是否启用审计功能
     */
    private boolean enabled = true;

    /**
     * 审计日志存储路径
     */
    @NotBlank
    private String storagePath = "logs/audit";

    /**
     * 日志保留天数
     */
    @Min(1)
    private int retentionDays = 180;

    /**
     * 批量写入大小
     */
    @Min(1)
    private int batchSize = 500;

    /**
     * 异步队列容量
     */
    @Min(1000)
    private int queueCapacity = 20000;

    /**
     * 刷新间隔
     */
    @NotNull
    private Duration flushInterval = Duration.ofMillis(500);

    /**
     * I/O 线程数
     */
    @Min(1)
    private int ioThreads = 2;

    /**
     * 文件滚动大小阈值（字节）
     */
    @Min(1048576) // 1MB
    private long rollSizeBytes = 67108864L; // 64MB

    /**
     * 文件滚动时间间隔
     */
    @NotNull
    private Duration rollInterval = Duration.ofHours(1);

    /**
     * AES-256 加密密钥（Base64 编码）
     */
    @NotBlank
    // 默认演示密钥（32 字节的 Base64 编码），生产环境务必覆盖
    private String encryptionKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /**
     * 数字签名算法
     * 例如：SHA256withRSA、SHA256withECDSA
     */
    @NotBlank
    private String signatureAlgorithm = "SHA256withRSA";

    /**
     * 哈希链算法
     */
    @NotBlank
    private String hashAlgorithm = "SHA-256";

    /**
     * 签名密钥文件路径
     */
    private String keyStorePath = "";

    /**
     * 签名密钥密码
     */
    private String keyStorePassword = "";

    /**
     * 签名密钥别名
     */
    private String keyAlias = "audit-key";

    /**
     * 签名密钥密码
     */
    private String keyPassword = "";

    /**
     * 是否启用压缩存储
     */
    private boolean enableCompression = true;

    /**
     * 是否启用多级存储
     */
    private boolean enableMultiTierStorage = false;

    /**
     * Redis 存储配置
     */
    private RedisStorage redis = new RedisStorage();

    /**
     * 数据库存储配置
     */
    private DatabaseStorage database = new DatabaseStorage();

    /**
     * 告警配置
     */
    private AlertingConfig alerting = new AlertingConfig();

    @Data
    public static class RedisStorage {
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 6379;
        private int database = 0;
        private String password = "";
        private int timeout = 3000;
        private int maxPoolSize = 10;
    }

    @Data
    public static class DatabaseStorage {
        private boolean enabled = false;
        private String url = "";
        private String username = "";
        private String password = "";
        private String driverClassName = "";
    }

    @Data
    public static class AlertingConfig {
        private boolean enabled = false;
        private int failureThreshold = 10;
        private Duration checkInterval = Duration.ofMinutes(1);
        private String webhookUrl = "";
    }

    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (enabled) {
            if (encryptionKeyBase64 == null || encryptionKeyBase64.isEmpty()) {
                throw new IllegalArgumentException("加密密钥不能为空");
            }

            // 验证密钥长度（Base64 解码后应该是 32 字节）
            byte[] keyBytes;
            try {
                keyBytes = java.util.Base64.getDecoder().decode(encryptionKeyBase64);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("加密密钥必须是有效的 Base64 编码", e);
            }

            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("AES-256 加密密钥必须是 32 字节长度");
            }

            if (batchSize < 1) {
                throw new IllegalArgumentException("批量大小必须大于 0");
            }

            if (queueCapacity < batchSize * 2) {
                throw new IllegalArgumentException("队列容量应该至少是批量大小的 2 倍");
            }
        }
    }
}
