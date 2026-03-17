package com.basebackend.security.config;

import com.basebackend.security.event.SecurityAuditEventPublisher;
import com.basebackend.security.filter.AuthenticationRateLimitFilter;
import com.basebackend.security.service.AuthenticationRateLimiter;
import com.basebackend.security.service.impl.RedisAuthenticationRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 认证速率限制自动配置
 * <p>
 * 仅在 {@code security.baseline.rate-limit.enabled=true}（默认）且 RedisTemplate 可用时生效。
 */
@Configuration
@ConditionalOnProperty(prefix = "security.baseline.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(RedisTemplate.class)
public class RateLimitConfiguration {

    @Autowired(required = false)
    private SecurityAuditEventPublisher auditEventPublisher;

    @Bean
    public AuthenticationRateLimiter authenticationRateLimiter(RedisTemplate<String, Object> redisTemplate,
                                                               SecurityBaselineProperties properties) {
        return new RedisAuthenticationRateLimiter(redisTemplate, properties);
    }

    @Bean
    public AuthenticationRateLimitFilter authenticationRateLimitFilter(AuthenticationRateLimiter rateLimiter) {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(rateLimiter);
        if (auditEventPublisher != null) {
            filter.setAuditEventPublisher(auditEventPublisher);
        }
        return filter;
    }
}
