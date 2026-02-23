package com.basebackend.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

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

    // ========== P2: 密钥轮换配置 ==========

    /**
     * 多密钥配置（kid -> secret），用于密钥轮换。
     * 如果为空则使用单密钥模式（jwt.secret）。
     */
    private Map<String, String> keys = new LinkedHashMap<>();

    /**
     * 当前用于签名的活跃密钥 ID。
     * 仅在 jwt.keys 非空时生效。
     */
    private String activeKeyId;

    /**
     * 旧密钥保留时间（毫秒，默认 7 天）。
     * 密钥被停用后在此时间内仍可用于验证旧 Token。
     */
    private long keyRotationGracePeriod = 604800000;

    // ========== P3-1: 多设备管理配置 ==========

    /**
     * 每用户最大设备数（默认 5），超出时踢掉最早登录的设备。
     * 设为 0 或负数表示不限制。
     */
    private int maxDevicesPerUser = 5;

    // ========== P3-2: 审计配置 ==========

    /**
     * 是否启用 Token 事件审计日志（默认 true）
     */
    private boolean auditEnabled = true;

    /**
     * 是否通过 ApplicationEventPublisher 发布 Spring 事件（默认 false）
     */
    private boolean auditPublishSpringEvents = false;

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
        // 多密钥模式时，校验 activeKeyId 和各密钥长度
        if (!keys.isEmpty()) {
            if (activeKeyId == null || !keys.containsKey(activeKeyId)) {
                throw new IllegalStateException(
                        "jwt.active-key-id must reference a key defined in jwt.keys. " +
                                "Available keys: " + keys.keySet());
            }
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                if (entry.getValue() == null || entry.getValue().length() < 32) {
                    throw new IllegalStateException(
                            "jwt.keys[" + entry.getKey() + "] must be at least 32 characters (256 bits).");
                }
            }
        }
    }
}
