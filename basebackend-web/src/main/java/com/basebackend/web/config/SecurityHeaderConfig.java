package com.basebackend.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 安全头配置属性
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Data
@Component
@ConfigurationProperties(prefix = "web.security.header")
public class SecurityHeaderConfig {

    /**
     * 是否启用安全头
     */
    private boolean enabled = true;

    /**
     * X-Frame-Options 值
     */
    private String frameOptions = "DENY";

    /**
     * X-Content-Type-Options 值
     */
    private String contentTypeOptions = "nosniff";

    /**
     * X-XSS-Protection 值
     */
    private String xssProtection = "1; mode=block";

    /**
     * Strict-Transport-Security 值
     */
    private String strictTransportSecurity = "max-age=31536000; includeSubDomains";

    /**
     * Content-Security-Policy 值
     */
    private String contentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'";

    /**
     * Referrer-Policy 值
     */
    private String referrerPolicy = "strict-origin-when-cross-origin";

    /**
     * Permissions-Policy 值
     */
    private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

    /**
     * 自定义安全头
     */
    private List<String> customHeaders = Arrays.asList();

    /**
     * 是否为 HSTS 配置 includeSubDomains
     */
    private boolean includeSubDomains = true;

    /**
     * 是否为 HSTS 配置 preload
     */
    private boolean preload = false;
}
