package com.basebackend.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全基线相关 Bean 配置
 */
@Configuration
@EnableConfigurationProperties(SecurityBaselineProperties.class)
public class SecurityBaselineConfiguration {

    @Bean
    public CsrfCookieFilter csrfCookieFilter() {
        return new CsrfCookieFilter();
    }

    @Bean
    public OriginValidationFilter originValidationFilter(SecurityBaselineProperties properties) {
        return new OriginValidationFilter(properties);
    }
}
