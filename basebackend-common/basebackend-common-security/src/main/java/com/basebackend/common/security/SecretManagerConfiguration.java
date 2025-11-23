package com.basebackend.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 密钥管理器配置
 */
@Configuration
@EnableConfigurationProperties(SecretManagerProperties.class)
public class SecretManagerConfiguration {
}
