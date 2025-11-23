package com.basebackend.logging.audit.config;

import com.basebackend.logging.audit.crypto.*;
import com.basebackend.logging.audit.metrics.AuditMetrics;
import com.basebackend.logging.audit.service.AuditService;
import com.basebackend.logging.audit.service.AuditVerificationService;
import com.basebackend.logging.audit.storage.AuditStorage;
import com.basebackend.logging.audit.storage.CompositeAuditStorage;
import com.basebackend.logging.audit.storage.FileAuditStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计系统 Spring Boot 自动配置
 *
 * 自动配置审计系统的所有核心组件。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
@EnableScheduling
@ConditionalOnProperty(value = "basebackend.logging.audit.enabled", matchIfMissing = true)
public class AuditAutoConfiguration {

    /**
     * 配置审计指标
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditMetrics auditMetrics(MeterRegistry registry) {
        log.info("初始化审计指标组件");
        return new AuditMetrics(registry);
    }

    /**
     * 配置 AES 加密器
     */
    @Bean
    @ConditionalOnMissingBean
    public AesEncryptor aesEncryptor(AuditProperties properties) {
        log.info("初始化 AES 加密器");
        byte[] keyBytes = Base64Utils.decode(properties.getEncryptionKeyBase64());
        return new AesEncryptor(keyBytes);
    }

    /**
     * 配置对象映射器
     */
    @Bean
    @ConditionalOnMissingBean(name = "auditObjectMapper")
    public ObjectMapper auditObjectMapper() {
        log.info("初始化审计对象映射器");
        return CryptoObjectMapper.newInstance();
    }

    /**
     * 配置哈希链计算器
     */
    @Bean
    @ConditionalOnMissingBean
    public HashChainCalculator hashChainCalculator(AuditProperties properties,
                                                   @Qualifier("auditObjectMapper") ObjectMapper auditObjectMapper) {
        log.info("初始化哈希链计算器，算法: {}", properties.getHashAlgorithm());
        return new HashChainCalculator(properties.getHashAlgorithm(), auditObjectMapper);
    }

    /**
     * 配置数字签名服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditSignatureService auditSignatureService(AuditProperties properties) throws NoSuchAlgorithmException, NoSuchAlgorithmException {
        log.info("初始化数字签名服务，算法: {}", properties.getSignatureAlgorithm());

        Map<String, KeyPair> initialKeys = new HashMap<>();
        Map<String, java.security.cert.Certificate> certificates = new HashMap<>();

        // 从密钥库加载（如果配置了）
        String keyStorePath = properties.getKeyStorePath();
        String keyStorePassword = properties.getKeyStorePassword();
        String keyAlias = properties.getKeyAlias();
        String keyPassword = properties.getKeyPassword();

        if (!keyStorePath.isEmpty()) {
            try {
                // 实际生产环境应该从密钥库加载
                log.info("尝试从密钥库加载签名密钥: {}", keyStorePath);
                // TODO: 实现密钥库加载逻辑
            } catch (Exception e) {
                log.warn("加载密钥库失败，使用临时密钥: {}", e.getMessage());
            }
        }

        // 如果没有加载到密钥，生成临时密钥对（仅用于开发测试）
        if (initialKeys.isEmpty()) {
            String signatureAlgorithm = properties.getSignatureAlgorithm();
            String keyAlgorithm = resolveKeyAlgorithm(signatureAlgorithm);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm);
            if (signatureAlgorithm.contains("RSA")) {
                keyPairGenerator.initialize(3072);
            } else if (signatureAlgorithm.contains("ECDSA")) {
                keyPairGenerator.initialize(256);
            }
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            initialKeys.put("dev-key", keyPair);
            log.warn("使用临时签名密钥，仅适用于开发环境，生产环境必须使用密钥库管理");
        }

        return new AuditSignatureService(
                properties.getSignatureAlgorithm(),
                initialKeys,
                certificates,
                initialKeys.keySet().iterator().next()
        );
    }

    /**
     * 配置文件审计存储
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditStorage fileAuditStorage(AuditProperties properties,
                                         ObjectMapper objectMapper,
                                         AesEncryptor aesEncryptor) {
        log.info("初始化文件审计存储，路径: {}", properties.getStoragePath());

        Path storagePath = Paths.get(properties.getStoragePath());

        return new FileAuditStorage(
                storagePath,
                objectMapper,
                aesEncryptor,
                properties.isEnableCompression(),
                properties.getRollSizeBytes(),
                properties.getRollInterval()
        );
    }

    /**
     * 配置复合审计存储
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditStorage auditStorage(AuditProperties properties,
                                     AuditStorage fileStorage) {
        AuditStorage storage = fileStorage;

        // 如果启用了多级存储
        if (properties.isEnableMultiTierStorage()) {
            List<AuditStorage> secondaries = new ArrayList<>();

            // Redis 存储
            if (properties.getRedis().isEnabled()) {
                log.info("初始化 Redis 审计存储");
                // TODO: 实现 Redis 存储
                // secondaries.add(new RedisAuditStorage(...));
            }

            // 数据库存储
            if (properties.getDatabase().isEnabled()) {
                log.info("初始化数据库审计存储");
                // TODO: 实现数据库存储
                // secondaries.add(new DatabaseAuditStorage(...));
            }

            if (!secondaries.isEmpty()) {
                storage = new CompositeAuditStorage(fileStorage, secondaries);
                log.info("初始化复合审计存储，主存储: {}, 备用存储: {}",
                        fileStorage.getClass().getSimpleName(),
                        secondaries.size());
            }
        }

        return storage;
    }

    /**
     * 配置审计服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditService auditService(AuditProperties properties,
                                     AuditStorage storage,
                                     HashChainCalculator hashChainCalculator,
                                     AuditSignatureService signatureService,
                                     AuditMetrics metrics) {
        log.info("初始化审计服务");

        AuditService auditService = new AuditService(
                storage,
                hashChainCalculator,
                signatureService,
                metrics,
                properties.getQueueCapacity(),
                properties.getBatchSize(),
                properties.getFlushInterval().toMillis()
        );

        // 验证配置
        properties.validate();

        log.info("审计服务初始化完成");
        return auditService;
    }

    /**
     * 配置审计验证服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditVerificationService auditVerificationService(HashChainCalculator hashChainCalculator,
                                                             AuditSignatureService signatureService) {
        log.info("初始化审计验证服务");
        return new AuditVerificationService(hashChainCalculator, signatureService);
    }

    /**
     * 注册关闭钩子
     */
    public void registerShutdownHook(AuditService auditService,
                                    AuditVerificationService verificationService) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM 关闭钩子执行，关闭审计服务");
            auditService.shutdown();
            verificationService.shutdown();
        }));
    }

    /**
     * 根据签名算法推导 KeyPairGenerator 算法，避免使用不被支持的名称（如 SHA256withRSA）。
     */
    private String resolveKeyAlgorithm(String signatureAlgorithm) {
        if (signatureAlgorithm == null) {
            throw new IllegalArgumentException("签名算法不能为空");
        }
        if (signatureAlgorithm.contains("RSA")) {
            return "RSA";
        }
        if (signatureAlgorithm.contains("ECDSA") || signatureAlgorithm.contains("EC")) {
            return "EC";
        }
        throw new IllegalArgumentException("不支持的签名算法: " + signatureAlgorithm);
    }
}
