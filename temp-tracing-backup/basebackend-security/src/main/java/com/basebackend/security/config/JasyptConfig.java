package com.basebackend.security.config;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.PBEConfig;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jasypt配置
 * 用于配置环境变量和敏感配置的加密
 */
@Slf4j
@Configuration
public class JasyptConfig {

    @Value("${jasypt.encryptor.password:basebackend-encrypt-password}")
    private String encryptorPassword;

    /**
     * 创建Jasypt字符串加密器
     * 用于加密配置文件中的敏感信息
     */
    @Bean
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        // 设置加密密码
        config.setPassword(encryptorPassword);

        // 设置算法
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");

        // 设置密钥生成器
        config.setKeyObtentionIterations(1000);
        config.setPoolSize(1);

        // 设置盐生成器
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");

        // 设置IV生成器
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");

        // 设置最后的验证器
        config.setStringOutputType("base64");
        config.setAppendingEncryptionMode("SALT");

        encryptor.setConfig(config);

        log.info("Jasypt字符串加密器初始化完成");
        return encryptor;
    }
}
