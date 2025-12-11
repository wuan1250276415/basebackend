package com.basebackend.featuretoggle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos配置属性
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "basebackend.feature-toggle.nacos")
public class NacosConfigProperties {

    /**
     * 是否启用Nacos配置
     */
    private boolean enabled = false;

    /**
     * Nacos服务器地址
     */
    private String serverAddr = "localhost:8848";

    /**
     * 命名空间
     */
    private String namespace = "public";

    /**
     * 分组ID
     */
    private String groupId = "DEFAULT_GROUP";

    /**
     * 特性开关配置的DataId
     */
    private String dataId = "feature-toggles-${spring.profiles.active}.properties";

    /**
     * 获取配置的超时时间（毫秒）
     */
    private long timeoutMs = 5000L;

    /**
     * 是否启用配置缓存
     */
    private boolean enableCache = true;

    /**
     * 缓存刷新间隔（毫秒）
     */
    private long cacheRefreshIntervalMs = 60000L; // 1分钟

    /**
     * 配置文件的编码格式
     */
    private String encoding = "UTF-8";

    /**
     * 是否启用配置变更监听
     */
    private boolean enableListening = true;

    /**
     * Nacos用户名（如果需要认证）
     */
    private String username;

    /**
     * Nacos密码（如果需要认证）
     */
    private String password;

    /**
     * 访问令牌（如果需要认证）
     */
    private String accessToken;

    /**
     * Context路径（Nacos 2.x支持）
     */
    private String contextPath = "/nacos";

    /**
     * 集群名称
     */
    private String clusterName = "default";

    /**
     * Endpoint（多环境支持）
     */
    private String endpoint;

    /**
     * 命名空间ID（与namespace的区别：namespace是名称，namespaceId是ID）
     */
    private String namespaceId;

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getDataId() {
        return dataId;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public long getCacheRefreshIntervalMs() {
        return cacheRefreshIntervalMs;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isEnableListening() {
        return enableListening;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getNamespaceId() {
        return namespaceId;
    }
}
