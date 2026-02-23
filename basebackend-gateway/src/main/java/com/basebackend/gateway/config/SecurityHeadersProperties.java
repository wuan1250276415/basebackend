package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.security.headers")
public class SecurityHeadersProperties {

    private boolean enabled = true;

    private long hstsMaxAge = 31536000L;

    private boolean hstsIncludeSubdomains = true;

    private String contentSecurityPolicy = "default-src 'self'";

    private String frameOptions = "DENY";

    private boolean contentTypeOptions = true;

    private String referrerPolicy = "strict-origin-when-cross-origin";

    private String permissionsPolicy = "";
}
