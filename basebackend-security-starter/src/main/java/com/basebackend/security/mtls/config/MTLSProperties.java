package com.basebackend.security.mtls.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.net.ssl.TrustManagerFactory;

/**
 * mTLS配置属性
 *
 * 提供mTLS双向TLS认证的配置属性绑定
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@ConfigurationProperties(prefix = "basebackend.security.mtls")
public class MTLSProperties {

    /**
     * 是否启用mTLS双向认证
     */
    private boolean enabled = false;

    /**
     * 客户端配置
     */
    private ClientConfig client = new ClientConfig();

    /**
     * 服务端配置
     */
    private ServerConfig server = new ServerConfig();

    /**
     * 全局配置
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * 客户端配置内部类
     */
    @Data
    public static class ClientConfig {
        /**
         * 客户端KeyStore路径
         * 例如: /etc/ssl/client.jks
         */
        private String keyStorePath;

        /**
         * 客户端KeyStore密码
         */
        private String keyStorePassword;

        /**
         * 客户端私钥密码
         */
        private String keyPassword;

        /**
         * KeyStore类型
         * 常用值: JKS, PKCS12
         */
        private String keyStoreType = "JKS";

        /**
         * 客户端TrustStore路径
         * 例如: /etc/ssl/client-trust.jks
         */
        private String trustStorePath;

        /**
         * 客户端TrustStore密码
         */
        private String trustStorePassword;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 30000;

        /**
         * 是否自动生成自签名证书（仅限开发环境）
         */
        private boolean generateSelfSigned = false;

        /**
         * 客户端通用名称（CN）
         * 生成自签名证书时使用
         */
        private String commonName = "BaseBackend Client";

        /**
         * 启用证书吊销检查
         */
        private boolean enableRevocationCheck = false;

        /**
         * 启用证书有效性检查
         */
        private boolean enableValidityCheck = true;

        /**
         * 证书有效期检查提前天数
         * 证书剩余有效期少于该值时发出警告
         */
        private int expiryWarningDays = 30;

        /**
         * 是否验证证书主机名
         */
        private boolean enableHostnameVerification = true;

        /**
         * 是否启用证书链验证
         */
        private boolean enableChainValidation = true;

        /**
         * 信任证书算法
         */
        private String trustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    }

    /**
     * 服务端配置内部类
     */
    @Data
    public static class ServerConfig {
        /**
         * 是否启用服务端mTLS
         */
        private boolean enabled = false;

        /**
         * 服务端KeyStore路径
         */
        private String keyStorePath;

        /**
         * 服务端KeyStore密码
         */
        private String keyStorePassword;

        /**
         * 服务端私钥密码
         */
        private String keyPassword;

        /**
         * KeyStore类型
         */
        private String keyStoreType = "JKS";

        /**
         * 服务端TrustStore路径（用于验证客户端证书）
         */
        private String trustStorePath;

        /**
         * 服务端TrustStore密码
         */
        private String trustStorePassword;

        /**
         * 服务端通用名称（CN）
         */
        private String commonName = "BaseBackend Server";

        /**
         * 客户端证书验证策略
         * 可选值:
         * - NONE: 不验证客户端证书
         * - OPTIONAL: 可选客户端证书
         * - REQUIRED: 必须提供客户端证书
         */
        private String clientAuth = "REQUIRED";

        /**
         * 是否要求客户端证书链完整
         */
        private boolean requireFullChain = true;

        /**
         * 允许的客户端证书主题过滤器
         * 使用正则表达式匹配
         */
        private String allowedSubjectPattern = ".*";

        /**
         * 允许的客户端证书颁发者过滤器
         * 使用正则表达式匹配
         */
        private String allowedIssuerPattern = ".*";

        /**
         * TLS协议版本
         * 可选值: TLSv1.2, TLSv1.3
         */
        private String[] enabledProtocols = {"TLSv1.2", "TLSv1.3"};

        /**
         * 启用的加密套件
         * 默认使用JDK推荐的安全套件
         */
        private String[] enabledCipherSuites = {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_GCM_SHA256"
        };

        /**
         * 是否启用客户端证书缓存
         */
        private boolean enableClientCertCache = true;

        /**
         * 客户端证书缓存大小
         */
        private int clientCertCacheSize = 1000;

        /**
         * 客户端证书缓存超时（秒）
         */
        private int clientCertCacheTimeout = 3600;
    }

    /**
     * 全局配置内部类
     */
    @Data
    public static class GlobalConfig {
        /**
         * 是否启用调试模式
         */
        private boolean debugEnabled = false;

        /**
         * 证书监控周期（分钟）
         * 定期检查证书有效期
         */
        private int monitoringIntervalMinutes = 60;

        /**
         * 证书过期告警阈值（天）
         */
        private int expiryAlertDays = 30;

        /**
         * 是否启用证书自动轮换
         */
        private boolean enableAutoRenewal = false;

        /**
         * 证书轮换提前天数
         */
        private int renewalDaysBeforeExpiry = 7;

        /**
         * 是否启用证书健康检查
         */
        private boolean enableHealthCheck = true;

        /**
         * 健康检查超时时间（秒）
         */
        private int healthCheckTimeout = 30;

        /**
         * 证书加载策略
         * 可选值:
         * - EAGER: 启动时立即加载
         * - LAZY: 首次使用时加载
         */
        private String loadStrategy = "EAGER";

        /**
         * 是否启用SSL Session缓存
         */
        private boolean enableSessionCache = true;

        /**
         * SSL Session缓存大小
         */
        private int sessionCacheSize = 100;

        /**
         * SSL Session超时时间（秒）
         */
        private int sessionTimeout = 300;
    }

    /**
     * 客户端认证模式枚举
     */
    public enum ClientAuthMode {
        NONE("NONE", "不验证客户端证书"),
        OPTIONAL("OPTIONAL", "可选客户端证书"),
        REQUIRED("REQUIRED", "必须提供客户端证书");

        private final String code;
        private final String description;

        ClientAuthMode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public static ClientAuthMode fromCode(String code) {
            for (ClientAuthMode mode : values()) {
                if (mode.getCode().equals(code)) {
                    return mode;
                }
            }
            return REQUIRED; // 默认值
        }
    }
}
