package com.basebackend.database.security.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.interceptor.DecryptionInterceptor;
import com.basebackend.database.security.interceptor.EncryptionInterceptor;
import com.basebackend.database.security.service.AlertService;
import com.basebackend.database.security.service.EncryptionService;
import com.basebackend.database.security.service.impl.AESEncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 加密功能配置类
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.security.encryption", name = "enabled", havingValue = "true")
public class EncryptionConfig {

    /**
     * 加密服务Bean
     */
    @Bean("encryptionService")
    public EncryptionService encryptionService(DatabaseEnhancedProperties properties) {
        log.info("Initializing encryption service with algorithm: {}",
                properties.getSecurity().getEncryption().getAlgorithm());
        return new AESEncryptionService(properties);
    }

    /**
     * 加密拦截器Bean
     */
    @Bean("encryptionInterceptor")
    public EncryptionInterceptor encryptionInterceptor(
            EncryptionService encryptionService,
            AlertService alertService,
            DatabaseEnhancedProperties properties) {
        log.info("Registering encryption interceptor");
        return new EncryptionInterceptor(encryptionService, alertService, properties);
    }

    /**
     * 解密拦截器Bean
     */
    @Bean("decryptionInterceptor")
    public DecryptionInterceptor decryptionInterceptor(
            EncryptionService encryptionService,
            DatabaseEnhancedProperties properties) {
        log.info("Registering decryption interceptor");
        return new DecryptionInterceptor(encryptionService, properties);
    }
}
