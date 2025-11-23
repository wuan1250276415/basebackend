package com.basebackend.security.oauth2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2资源服务器配置属性
 *
 * 提供OAuth2资源服务器的配置属性绑定
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@ConfigurationProperties(prefix = "basebackend.security.oauth2")
public class OAuth2ResourceServerProperties {

    /**
     * 是否启用OAuth2资源服务器
     */
    private boolean enabled = false;

    /**
     * JWT配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * 认证服务器配置
     */
    private AuthorizationServerConfig authorizationServer = new AuthorizationServerConfig();

    /**
     * 资源服务器配置
     */
    private ResourceServerConfig resourceServer = new ResourceServerConfig();

    /**
     * JWT配置内部类
     */
    @Data
    public static class JwtConfig {
        /**
         * JWK Set URI - 用于获取公钥验证Token签名
         * 例如: https://auth.example.com/oauth2/jwks
         */
        private String jwkSetUri;

        /**
         * Token签发者URI
         * 例如: https://auth.example.com/oauth2
         */
        private String issuerUri;

        /**
         * Token受众（audience）
         */
        private String audience;

        /**
         * Token验证的超时时间
         */
        private int clockSkew = 300; // 5分钟，默认允许5秒的时间偏差

        /**
         * 是否缓存JWK Set
         */
        private boolean cacheJwkSet = true;

        /**
         * JWK Set缓存时间（秒）
         */
        private int jwkSetCacheTtl = 3600; // 1小时

        /**
         * JWK Set缓存大小
         */
        private int jwkSetCacheSize = 5;
    }

    /**
     * 授权服务器配置
     */
    @Data
    public static class AuthorizationServerConfig {
        /**
         * 授权服务器URL
         * 例如: https://auth.example.com
         */
        private String url;

        /**
         * 客户端ID
         */
        private String clientId;

        /**
         * 客户端密钥
         */
        private String clientSecret;

        /**
         * 授权服务器元数据URI
         * 例如: https://auth.example.com/oauth2/.well-known/openid-configuration
         */
        private String metadataUri;

        /**
         * Token URI
         * 例如: https://auth.example.com/oauth2/token
         */
        private String tokenUri;

        /**
         * 授权URI
         * 例如: https://auth.example.com/oauth2/authorize
         */
        private String authorizationUri;

        /**
         * 用户信息URI
         * 例如: https://auth.example.com/oauth2/userinfo
         */
        private String userInfoUri;

        /**
         * 是否启用PKCE（Proof Key for Code Exchange）
         */
        private boolean enablePkce = true;

        /**
         * Token刷新策略
         */
        private TokenRefreshConfig refreshToken = new TokenRefreshConfig();
    }

    /**
     * Token刷新配置
     */
    @Data
    public static class TokenRefreshConfig {
        /**
         * 是否启用Token自动刷新
         */
        private boolean enabled = true;

        /**
         * Token刷新阈值（秒）
         * Token剩余有效期低于此值时触发刷新
         */
        private int refreshThreshold = 300; // 5分钟

        /**
         * 刷新Token请求超时时间（毫秒）
         */
        private int timeout = 10000; // 10秒

        /**
         * 刷新Token重试次数
         */
        private int retryCount = 3;

        /**
         * 刷新Token重试间隔（毫秒）
         */
        private int retryInterval = 1000; // 1秒
    }

    /**
     * 资源服务器配置
     */
    @Data
    public static class ResourceServerConfig {
        /**
         * 资源ID列表
         * 例如: ["api://scheduler", "api://system"]
         */
        private String[] resourceIds = {};

        /**
         * 权限提取策略
         */
        private PermissionStrategyConfig permissionStrategy = new PermissionStrategyConfig();

        /**
         * 令牌时效性验证配置
         */
        private TokenValidationConfig tokenValidation = new TokenValidationConfig();

        /**
         * 缓存配置
         */
        private CacheConfig cache = new CacheConfig();
    }

    /**
     * 权限提取策略配置
     */
    @Data
    public static class PermissionStrategyConfig {
        /**
         * 权限字段优先级配置
         * 字段名按优先级排序
         */
        private String[] fieldsPriority = {
            "permissions",
            "roles",
            "scopes",
            "authorities"
        };

        /**
         * 是否使用通配符权限匹配
         * 例如: "system:dept:*" 匹配 "system:dept:create"
         */
        private boolean enableWildcardMatch = true;

        /**
         * 通配符匹配的分隔符
         */
        private String wildcardSeparator = ":";

        /**
         * 是否启用角色权限继承
         * 例如: 角色admin继承所有权限
         */
        private boolean enableRoleInheritance = true;

        /**
         * 超级管理员角色列表
         */
        private String[] superAdminRoles = {"admin", "super_admin", "root"};
    }

    /**
     * Token验证配置
     */
    @Data
    public static class TokenValidationConfig {
        /**
         * 是否验证Token时效性
         */
        private boolean enableExpirationCheck = true;

        /**
         * 是否验证Token签发者
         */
        private boolean enableIssuerCheck = true;

        /**
         * 是否验证Token受众
         */
        private boolean enableAudienceCheck = true;

        /**
         * 是否验证Token用途（scopes）
         */
        private boolean enableScopeCheck = true;

        /**
         * 是否启用Token吊销检查
         */
        private boolean enableRevocationCheck = false;

        /**
         * Token吊销检查服务端点
         */
        private String revocationCheckEndpoint;

        /**
         * 时间偏差容忍（秒）
         */
        private int clockSkew = 300; // 5分钟
    }

    /**
     * 缓存配置
     */
    @Data
    public static class CacheConfig {
        /**
         * 是否启用权限缓存
         */
        private boolean enabled = true;

        /**
         * 缓存过期时间（秒）
         */
        private int expireTime = 1800; // 30分钟

        /**
         * 最大缓存大小
         */
        private int maxSize = 10000;

        /**
         * 缓存键前缀
         */
        private String keyPrefix = "oauth2:permission:";

        /**
         * 是否使用用户ID作为缓存键
         */
        private boolean useUserIdAsKey = true;
    }
}
