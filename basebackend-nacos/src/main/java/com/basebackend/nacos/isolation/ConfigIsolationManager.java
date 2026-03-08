/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.isolation;

import com.basebackend.nacos.isolation.ConfigIsolationContext;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConfigIsolationManager {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ConfigIsolationManager.class);

    public ConfigIsolationContext createContext(String environment, String tenantId, Long appId) {
        ConfigIsolationContext context = new ConfigIsolationContext();
        context.setEnvironment(environment);
        context.setTenantId(tenantId);
        context.setAppId(appId);
        return context;
    }

    public String parseOriginalDataId(String fullDataId) {
        if (fullDataId == null || fullDataId.isEmpty()) {
            return fullDataId;
        }
        String[] parts = fullDataId.split("/");
        return parts[parts.length - 1];
    }

    public boolean validateContext(ConfigIsolationContext context) {
        if (context == null) {
            log.warn("\u914d\u7f6e\u9694\u79bb\u4e0a\u4e0b\u6587\u4e3a\u7a7a");
            return false;
        }
        return context.getEnvironment() != null || context.getTenantId() != null || context.getAppId() != null;
    }

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

