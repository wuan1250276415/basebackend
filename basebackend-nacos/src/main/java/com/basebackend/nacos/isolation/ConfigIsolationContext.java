package com.basebackend.nacos.isolation;

import lombok.Data;

/**
 * 配置隔离上下文
 * 用于多维度配置隔离
 */
@Data
public class ConfigIsolationContext {

    /**
     * 环境（dev/test/prod等）
     */
    private String environment;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 分组
     */
    private String group;

    /**
     * 构建配置的完整Data ID
     * 格式：{environment}/{tenantId}/{appId}/{originalDataId}
     */
    public String buildDataId(String originalDataId) {
        StringBuilder dataId = new StringBuilder();

        if (environment != null && !environment.isEmpty()) {
            dataId.append(environment).append("/");
        }

        if (tenantId != null && !tenantId.isEmpty()) {
            dataId.append(tenantId).append("/");
        }

        if (appId != null) {
            dataId.append("app_").append(appId).append("/");
        }

        dataId.append(originalDataId);

        return dataId.toString();
    }

    /**
     * 构建命名空间
     * 环境隔离通过namespace实现
     */
    public String buildNamespace() {
        if (namespace != null && !namespace.isEmpty()) {
            return namespace;
        }

        // 默认使用环境作为命名空间
        if (environment != null && !environment.isEmpty()) {
            return environment;
        }

        return "public";
    }

    /**
     * 构建分组
     * 租户/应用隔离可以通过group实现
     */
    public String buildGroup() {
        if (group != null && !group.isEmpty()) {
            return group;
        }

        // 默认使用租户_应用作为分组
        if (tenantId != null && appId != null) {
            return tenantId + "_" + appId;
        } else if (tenantId != null) {
            return tenantId;
        } else if (appId != null) {
            return "app_" + appId;
        }

        return "DEFAULT_GROUP";
    }
}
