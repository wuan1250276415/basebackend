package com.basebackend.gateway.config;

import com.basebackend.common.security.SecurityBaselineProperties;
import com.basebackend.common.security.SecretManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Gateway Security配置
 * 为Gateway提供必要的安全组件，但不使用Servlet相关的Filter
 */
@Configuration
@EnableConfigurationProperties(SecurityBaselineProperties.class)
public class GatewaySecurityConfig {

    /**
     * 为JwtUtil提供SecretManager
     * Gateway只需要SecretManager来读取JWT密钥
     */
    @Bean
    public SecretManager secretManager(ConfigurableEnvironment environment,
                                      SecurityBaselineProperties properties) {
        return new SecretManager(environment, properties);
    }
}