package com.basebackend.common.audit.config;

import com.basebackend.common.audit.AuditEventPublisher;
import com.basebackend.common.audit.aspect.AuditLogAspect;
import com.basebackend.common.audit.impl.SpringAuditEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(prefix = "basebackend.common.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringAuditEventPublisher(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogAspect auditLogAspect(AuditEventPublisher auditEventPublisher,
                                          @Nullable HttpServletRequest httpServletRequest) {
        return new AuditLogAspect(auditEventPublisher, httpServletRequest);
    }
}
