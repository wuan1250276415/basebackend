package com.basebackend.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 密钥管理器配置项
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.secret-manager")
public class SecretManagerProperties {

    /**
     * 密钥缓存有效期，默认15分钟
     */
    private Duration cacheTtl = Duration.ofMinutes(15);
}
