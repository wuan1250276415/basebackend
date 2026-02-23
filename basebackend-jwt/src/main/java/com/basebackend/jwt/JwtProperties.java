package com.basebackend.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性，集中管理所有 JWT 相关配置项
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥（至少 32 字符 / 256 bits）
     */
    private String secret;

    /**
     * Token 签发者
     */
    private String issuer = "basebackend";

    /**
     * Token 受众
     */
    private String audience;

    /**
     * 通用 Token 过期时间（毫秒），向后兼容旧 generateToken 方法（默认 24 小时）
     */
    private long expiration = 86400000;

    /**
     * Access Token 过期时间（毫秒，默认 30 分钟）
     */
    private long accessTokenExpiration = 1800000;

    /**
     * Refresh Token 过期时间（毫秒，默认 7 天）
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * Token 即将过期判定阈值（毫秒，默认 1 小时）
     */
    private long expiringSoonThreshold = 3600000;

    /**
     * 允许刷新已过期 Token 的最大过期时长（毫秒，默认 7 天）
     */
    private long maxRefreshableExpiredDuration = 604800000;

    /**
     * 是否将权限列表放入 Token
     */
    private boolean includePermissions = false;

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret must be configured. Refusing to start with no JWT signing secret.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters (256 bits) for HMAC-SHA256. " +
                            "Current length: " + secret.length());
        }
    }
}
