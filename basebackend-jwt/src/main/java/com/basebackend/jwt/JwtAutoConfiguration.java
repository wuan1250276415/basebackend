package com.basebackend.jwt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

/**
 * JWT 模块自动配置
 * <p>
 * 注册 JwtTokenBlacklist、JwtKeyManager、JwtDeviceManager、JwtAuditLogger、JwtUtil。
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnProperty(prefix = "basebackend.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenBlacklist jwtTokenBlacklist(
            @Nullable StringRedisTemplate redisTemplate) {
        return new JwtTokenBlacklist(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtKeyManager jwtKeyManager(JwtProperties properties) {
        return new JwtKeyManager(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDeviceManager jwtDeviceManager(
            @Nullable StringRedisTemplate redisTemplate,
            JwtProperties properties) {
        return new JwtDeviceManager(redisTemplate, properties.getMaxDevicesPerUser());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuditLogger jwtAuditLogger(
            JwtProperties properties,
            @Nullable ApplicationEventPublisher eventPublisher) {
        return new JwtAuditLogger(
                properties.isAuditEnabled(),
                properties.isAuditPublishSpringEvents(),
                eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil(JwtProperties properties,
                           JwtKeyManager keyManager,
                           JwtTokenBlacklist blacklist,
                           @Nullable JwtDeviceManager deviceManager,
                           @Nullable JwtAuditLogger auditLogger) {
        return new JwtUtil(properties, keyManager, blacklist, deviceManager, auditLogger);
    }
}
