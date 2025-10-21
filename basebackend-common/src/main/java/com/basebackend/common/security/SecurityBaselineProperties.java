package com.basebackend.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全基线配置项
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.baseline")
public class SecurityBaselineProperties {

    /**
     * 允许的请求来源（Origin 或 Referer 前缀），为空时不强制校验
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 是否强制校验 Referer，当 Origin 缺失时生效
     */
    private boolean enforceReferer = true;

    /**
     * 密钥缓存有效期
     */
    private Duration secretCacheTtl = Duration.ofMinutes(15);
}
