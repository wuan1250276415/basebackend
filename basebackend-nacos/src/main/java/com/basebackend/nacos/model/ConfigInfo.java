/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.springframework.util.StringUtils
 */
package com.basebackend.nacos.model;

import java.io.Serializable;
import lombok.Generated;
import org.springframework.util.StringUtils;

public class ConfigInfo
implements Serializable {
    private Long id;
    private String dataId;
    private String group;
    private String namespace;
    private String content;
    private String type;
    private String environment;
    private String tenantId;
    private Long appId;
    private Integer version;
    private String status;
    private Boolean isCritical;
    private String publishType;
    private String description;
    private String md5;

    public String getServiceName() {
        if (!StringUtils.hasText((String)this.dataId)) {
            return null;
        }
        if (this.dataId.contains("-")) {
            return this.dataId.split("-")[0];
        }
        return this.dataId;
    }

    @Generated
    public static ConfigInfoBuilder builder() {
        return new ConfigInfoBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getDataId() {
        return this.dataId;
    }

    @Generated
    public String getGroup() {
        return this.group;
    }

    @Generated
    public String getNamespace() {
        return this.namespace;
    }

    @Generated
    public String getContent() {
        return this.content;
    }

    @Generated
    public String getType() {
        return this.type;
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
    public Integer getVersion() {
        return this.version;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public Boolean getIsCritical() {
        return this.isCritical;
    }

    @Generated
    public String getPublishType() {
        return this.publishType;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getMd5() {
        return this.md5;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    @Generated
    public void setGroup(String group) {
        this.group = group;
    }

    @Generated
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Generated
    public void setContent(String content) {
        this.content = content;
    }

    @Generated
    public void setType(String type) {
        this.type = type;
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
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setIsCritical(Boolean isCritical) {
        this.isCritical = isCritical;
    }

    @Generated
    public void setPublishType(String publishType) {
        this.publishType = publishType;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ConfigInfo)) {
            return false;
        }
        ConfigInfo other = (ConfigInfo)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$appId = this.getAppId();
        Long other$appId = other.getAppId();
        if (this$appId == null ? other$appId != null : !((Object)this$appId).equals(other$appId)) {
            return false;
        }
        Integer this$version = this.getVersion();
        Integer other$version = other.getVersion();
        if (this$version == null ? other$version != null : !((Object)this$version).equals(other$version)) {
            return false;
        }
        Boolean this$isCritical = this.getIsCritical();
        Boolean other$isCritical = other.getIsCritical();
        if (this$isCritical == null ? other$isCritical != null : !((Object)this$isCritical).equals(other$isCritical)) {
            return false;
        }
        String this$dataId = this.getDataId();
        String other$dataId = other.getDataId();
        if (this$dataId == null ? other$dataId != null : !this$dataId.equals(other$dataId)) {
            return false;
        }
        String this$group = this.getGroup();
        String other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) {
            return false;
        }
        String this$namespace = this.getNamespace();
        String other$namespace = other.getNamespace();
        if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
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
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$publishType = this.getPublishType();
        String other$publishType = other.getPublishType();
        if (this$publishType == null ? other$publishType != null : !this$publishType.equals(other$publishType)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$md5 = this.getMd5();
        String other$md5 = other.getMd5();
        return !(this$md5 == null ? other$md5 != null : !this$md5.equals(other$md5));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ConfigInfo;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $appId = this.getAppId();
        result = result * 59 + ($appId == null ? 43 : ((Object)$appId).hashCode());
        Integer $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : ((Object)$version).hashCode());
        Boolean $isCritical = this.getIsCritical();
        result = result * 59 + ($isCritical == null ? 43 : ((Object)$isCritical).hashCode());
        String $dataId = this.getDataId();
        result = result * 59 + ($dataId == null ? 43 : $dataId.hashCode());
        String $group = this.getGroup();
        result = result * 59 + ($group == null ? 43 : $group.hashCode());
        String $namespace = this.getNamespace();
        result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $environment = this.getEnvironment();
        result = result * 59 + ($environment == null ? 43 : $environment.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $publishType = this.getPublishType();
        result = result * 59 + ($publishType == null ? 43 : $publishType.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $md5 = this.getMd5();
        result = result * 59 + ($md5 == null ? 43 : $md5.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ConfigInfo(id=" + this.getId() + ", dataId=" + this.getDataId() + ", group=" + this.getGroup() + ", namespace=" + this.getNamespace() + ", content=" + this.getContent() + ", type=" + this.getType() + ", environment=" + this.getEnvironment() + ", tenantId=" + this.getTenantId() + ", appId=" + this.getAppId() + ", version=" + this.getVersion() + ", status=" + this.getStatus() + ", isCritical=" + this.getIsCritical() + ", publishType=" + this.getPublishType() + ", description=" + this.getDescription() + ", md5=" + this.getMd5() + ")";
    }

    @Generated
    public ConfigInfo() {
    }

    @Generated
    public ConfigInfo(Long id, String dataId, String group, String namespace, String content, String type, String environment, String tenantId, Long appId, Integer version, String status, Boolean isCritical, String publishType, String description, String md5) {
        this.id = id;
        this.dataId = dataId;
        this.group = group;
        this.namespace = namespace;
        this.content = content;
        this.type = type;
        this.environment = environment;
        this.tenantId = tenantId;
        this.appId = appId;
        this.version = version;
        this.status = status;
        this.isCritical = isCritical;
        this.publishType = publishType;
        this.description = description;
        this.md5 = md5;
    }

    @Generated
    public static class ConfigInfoBuilder {
        @Generated
        private Long id;
        @Generated
        private String dataId;
        @Generated
        private String group;
        @Generated
        private String namespace;
        @Generated
        private String content;
        @Generated
        private String type;
        @Generated
        private String environment;
        @Generated
        private String tenantId;
        @Generated
        private Long appId;
        @Generated
        private Integer version;
        @Generated
        private String status;
        @Generated
        private Boolean isCritical;
        @Generated
        private String publishType;
        @Generated
        private String description;
        @Generated
        private String md5;

        @Generated
        ConfigInfoBuilder() {
        }

        @Generated
        public ConfigInfoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public ConfigInfoBuilder dataId(String dataId) {
            this.dataId = dataId;
            return this;
        }

        @Generated
        public ConfigInfoBuilder group(String group) {
            this.group = group;
            return this;
        }

        @Generated
        public ConfigInfoBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        @Generated
        public ConfigInfoBuilder content(String content) {
            this.content = content;
            return this;
        }

        @Generated
        public ConfigInfoBuilder type(String type) {
            this.type = type;
            return this;
        }

        @Generated
        public ConfigInfoBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }

        @Generated
        public ConfigInfoBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Generated
        public ConfigInfoBuilder appId(Long appId) {
            this.appId = appId;
            return this;
        }

        @Generated
        public ConfigInfoBuilder version(Integer version) {
            this.version = version;
            return this;
        }

        @Generated
        public ConfigInfoBuilder status(String status) {
            this.status = status;
            return this;
        }

        @Generated
        public ConfigInfoBuilder isCritical(Boolean isCritical) {
            this.isCritical = isCritical;
            return this;
        }

        @Generated
        public ConfigInfoBuilder publishType(String publishType) {
            this.publishType = publishType;
            return this;
        }

        @Generated
        public ConfigInfoBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Generated
        public ConfigInfoBuilder md5(String md5) {
            this.md5 = md5;
            return this;
        }

        @Generated
        public ConfigInfo build() {
            return new ConfigInfo(this.id, this.dataId, this.group, this.namespace, this.content, this.type, this.environment, this.tenantId, this.appId, this.version, this.status, this.isCritical, this.publishType, this.description, this.md5);
        }

        @Generated
        public String toString() {
            return "ConfigInfo.ConfigInfoBuilder(id=" + this.id + ", dataId=" + this.dataId + ", group=" + this.group + ", namespace=" + this.namespace + ", content=" + this.content + ", type=" + this.type + ", environment=" + this.environment + ", tenantId=" + this.tenantId + ", appId=" + this.appId + ", version=" + this.version + ", status=" + this.status + ", isCritical=" + this.isCritical + ", publishType=" + this.publishType + ", description=" + this.description + ", md5=" + this.md5 + ")";
        }
    }
}

