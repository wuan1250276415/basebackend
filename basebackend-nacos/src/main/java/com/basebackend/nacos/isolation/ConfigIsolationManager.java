package com.basebackend.nacos.isolation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 配置隔离管理器
 * 统一管理配置的多维度隔离
 */
@Slf4j
@Component
public class ConfigIsolationManager {

    /**
     * 创建配置隔离上下文
     */
    public ConfigIsolationContext createContext(String environment, String tenantId, Long appId) {
        ConfigIsolationContext context = new ConfigIsolationContext();
        context.setEnvironment(environment);
        context.setTenantId(tenantId);
        context.setAppId(appId);
        return context;
    }

    /**
     * 解析Data ID获取原始配置名
     * 从 {environment}/{tenantId}/{appId}/{originalDataId} 格式中提取originalDataId
     */
    public String parseOriginalDataId(String fullDataId) {
        if (fullDataId == null || fullDataId.isEmpty()) {
            return fullDataId;
        }

        // 按 / 分割，取最后一部分
        String[] parts = fullDataId.split("/");
        return parts[parts.length - 1];
    }

    /**
     * 验证隔离上下文
     */
    public boolean validateContext(ConfigIsolationContext context) {
        if (context == null) {
            log.warn("配置隔离上下文为空");
            return false;
        }

        // 至少需要指定一个隔离维度
        return context.getEnvironment() != null ||
                context.getTenantId() != null ||
                context.getAppId() != null;
    }

    /**
     * 合并隔离上下文（优先使用override中的非空值）
     */
    public ConfigIsolationContext merge(ConfigIsolationContext base, ConfigIsolationContext override) {
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }

        ConfigIsolationContext merged = new ConfigIsolationContext();
        merged.setEnvironment(override.getEnvironment() != null ? override.getEnvironment() : base.getEnvironment());
        merged.setTenantId(override.getTenantId() != null ? override.getTenantId() : base.getTenantId());
        merged.setAppId(override.getAppId() != null ? override.getAppId() : base.getAppId());
        merged.setNamespace(override.getNamespace() != null ? override.getNamespace() : base.getNamespace());
        merged.setGroup(override.getGroup() != null ? override.getGroup() : base.getGroup());

        return merged;
    }
}
