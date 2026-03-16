/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.isolation;

import lombok.Generated;

public class ConfigIsolationContext {
    private String environment;
    private String tenantId;
    private Long appId;
    private String namespace;
    private String group;

    public String buildDataId(String originalDataId) {
        StringBuilder dataId = new StringBuilder();
        if (this.environment != null && !this.environment.isEmpty()) {
            dataId.append(this.environment).append("/");
        }
        if (this.tenantId != null && !this.tenantId.isEmpty()) {
            dataId.append(this.tenantId).append("/");
        }
        if (this.appId != null) {
            dataId.append("app_").append(this.appId).append("/");
        }
        dataId.append(originalDataId);
        return dataId.toString();
    }

    public String buildNamespace() {
        if (this.namespace != null && !this.namespace.isEmpty()) {
            return this.namespace;
        }
        if (this.environment != null && !this.environment.isEmpty()) {
            return this.environment;
        }
        return "public";
    }

    public String buildGroup() {
        if (this.group != null && !this.group.isEmpty()) {
            return this.group;
        }
        if (this.tenantId != null && this.appId != null) {
            return this.tenantId + "_" + this.appId;
        }
        if (this.tenantId != null) {
            return this.tenantId;
        }
        if (this.appId != null) {
            return "app_" + this.appId;
        }
        return "DEFAULT_GROUP";
    }

    @Generated
    public ConfigIsolationContext() {
    }

    @Generated
    public String getEnvironment() {
        return this.environment;
    }

    @Generated
    public String getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getAppId() {
        return this.appId;
    }

    @Generated
    public String getNamespace() {
        return this.namespace;
    }

    @Generated
    public String getGroup() {
        return this.group;
    }

    @Generated
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Generated
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setAppId(Long appId) {
        this.appId = appId;
    }

    @Generated
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Generated
    public void setGroup(String group) {
        this.group = group;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ConfigIsolationContext)) {
            return false;
        }
        ConfigIsolationContext other = (ConfigIsolationContext)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$appId = this.getAppId();
        Long other$appId = other.getAppId();
        if (this$appId == null ? other$appId != null : !((Object)this$appId).equals(other$appId)) {
            return false;
        }
        String this$environment = this.getEnvironment();
        String other$environment = other.getEnvironment();
        if (this$environment == null ? other$environment != null : !this$environment.equals(other$environment)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        String this$namespace = this.getNamespace();
        String other$namespace = other.getNamespace();
        if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace)) {
            return false;
        }
        String this$group = this.getGroup();
        String other$group = other.getGroup();
        return !(this$group == null ? other$group != null : !this$group.equals(other$group));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ConfigIsolationContext;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $appId = this.getAppId();
        result = result * 59 + ($appId == null ? 43 : ((Object)$appId).hashCode());
        String $environment = this.getEnvironment();
        result = result * 59 + ($environment == null ? 43 : $environment.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        String $namespace = this.getNamespace();
        result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
        String $group = this.getGroup();
        result = result * 59 + ($group == null ? 43 : $group.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ConfigIsolationContext(environment=" + this.getEnvironment() + ", tenantId=" + this.getTenantId() + ", appId=" + this.getAppId() + ", namespace=" + this.getNamespace() + ", group=" + this.getGroup() + ")";
    }
}

