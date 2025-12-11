package com.basebackend.featuretoggle.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 特性开关上下文。
 * <p>
 * 用于传递用户、租户、会话、环境等信息，供各特性开关提供商进行规则匹配。
 * 该类为不可变对象，线程安全，可安全在异步或并发场景下复用。
 */
public final class FeatureContext {

    /**
     * 用户ID
     */
    private final String userId;

    /**
     * 租户ID
     */
    private final String tenantId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 用户邮箱
     */
    private final String email;

    /**
     * 会话ID
     */
    private final String sessionId;

    /**
     * 客户端IP地址
     */
    private final String ipAddress;

    /**
     * 环境标识（dev/test/prod）
     */
    private final String environment;

    /**
     * 其他自定义属性
     */
    private final Map<String, String> properties;

    private FeatureContext(Builder builder) {
        this.userId = builder.userId;
        this.tenantId = builder.tenantId;
        this.username = builder.username;
        this.email = builder.email;
        this.sessionId = builder.sessionId;
        this.ipAddress = builder.ipAddress;
        this.environment = builder.environment;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
    }

    /**
     * 创建默认上下文（无任何用户信息）。
     *
     * @return 默认空上下文
     */
    public static FeatureContext empty() {
        return new Builder().build();
    }

    /**
     * 创建仅包含用户ID的上下文。
     *
     * @param userId 用户ID
     * @return 构建的上下文
     */
    public static FeatureContext forUser(String userId) {
        return new Builder().userId(userId).build();
    }

    /**
     * 创建包含基础用户信息的上下文。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param email    用户邮箱
     * @return 构建的上下文
     */
    public static FeatureContext forUser(String userId, String username, String email) {
        return new Builder()
                .userId(userId)
                .username(username)
                .email(email)
                .build();
    }

    /**
     * 复制当前上下文以便增量修改。
     *
     * @return 可进一步构建的新构建器
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getEnvironment() {
        return environment;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * 基于当前上下文新增单个属性并返回新实例。
     *
     * @param key   属性名
     * @param value 属性值
     * @return 新的上下文实例
     */
    public FeatureContext withProperty(String key, String value) {
        return toBuilder().property(key, value).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureContext that = (FeatureContext) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(username, that.username)
                && Objects.equals(email, that.email)
                && Objects.equals(sessionId, that.sessionId)
                && Objects.equals(ipAddress, that.ipAddress)
                && Objects.equals(environment, that.environment)
                && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tenantId, username, email, sessionId, ipAddress, environment, properties);
    }

    @Override
    public String toString() {
        return "FeatureContext{" +
                "userId='" + userId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", environment='" + environment + '\'' +
                ", properties=" + properties +
                '}';
    }

    /**
     * 构建器：用于灵活构建 {@link FeatureContext}。
     */
    public static final class Builder {
        private String userId;
        private String tenantId;
        private String username;
        private String email;
        private String sessionId;
        private String ipAddress;
        private String environment;
        private Map<String, String> properties = new HashMap<>();

        private Builder() {
        }

        private Builder(FeatureContext source) {
            this.userId = source.userId;
            this.tenantId = source.tenantId;
            this.username = source.username;
            this.email = source.email;
            this.sessionId = source.sessionId;
            this.ipAddress = source.ipAddress;
            this.environment = source.environment;
            this.properties = new HashMap<>(source.properties);
        }

        /**
         * 设置用户ID。
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置租户ID。
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * 设置用户名。
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * 设置用户邮箱。
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * 设置会话ID。
         */
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * 设置客户端IP。
         */
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * 设置环境标识。
         */
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        /**
         * 批量设置自定义属性，覆盖现有属性。
         */
        public Builder properties(Map<String, String> properties) {
            this.properties.clear();
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }

        /**
         * 新增单个自定义属性。
         */
        public Builder property(String key, String value) {
            if (key != null && value != null) {
                this.properties.put(key, value);
            }
            return this;
        }

        /**
         * 构建最终的 {@link FeatureContext} 实例。
         */
        public FeatureContext build() {
            return new FeatureContext(this);
        }
    }

    /**
     * 创建新的构建器实例。
     *
     * @return 新的构建器
     */
    public static Builder builder() {
        return new Builder();
    }
}
