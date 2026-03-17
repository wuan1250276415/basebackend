package com.basebackend.security.config;

import com.basebackend.security.event.SecurityAuditEventPublisher;
import com.basebackend.security.filter.CsrfCookieFilter;
import com.basebackend.security.filter.OriginValidationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全基线相关 Bean 配置
 */
@Configuration
@EnableConfigurationProperties(SecurityBaselineProperties.class)
public class SecurityBaselineConfiguration {

    @Autowired(required = false)
    private SecurityAuditEventPublisher auditEventPublisher;

    @Bean
    public CsrfCookieFilter csrfCookieFilter() {
        return new CsrfCookieFilter();
    }

    @Bean
    public OriginValidationFilter originValidationFilter(SecurityBaselineProperties properties) {
        OriginValidationFilter filter = new OriginValidationFilter(properties);
        if (auditEventPublisher != null) {
            filter.setAuditEventPublisher(auditEventPublisher);
        }
        return filter;
    }
}
