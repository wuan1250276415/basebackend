/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Generated;

public class ConfigHistory
implements Serializable {
    private Long id;
    private Long configId;
    private String dataId;
    private String group;
    private String namespace;
    private String content;
    private Integer version;
    private String operationType;
    private String operator;
    private Integer rollbackFrom;
    private LocalDateTime createTime;
    private String md5;

    @Generated
    public static ConfigHistoryBuilder builder() {
        return new ConfigHistoryBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public Long getConfigId() {
        return this.configId;
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
    public Integer getVersion() {
        return this.version;
    }

    @Generated
    public String getOperationType() {
        return this.operationType;
    }

    @Generated
    public String getOperator() {
        return this.operator;
    }

    @Generated
    public Integer getRollbackFrom() {
        return this.rollbackFrom;
    }

    @Generated
    public LocalDateTime getCreateTime() {
        return this.createTime;
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
    public void setConfigId(Long configId) {
        this.configId = configId;
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
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Generated
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    @Generated
    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Generated
    public void setRollbackFrom(Integer rollbackFrom) {
        this.rollbackFrom = rollbackFrom;
    }

    @Generated
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
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
        if (!(o instanceof ConfigHistory)) {
            return false;
        }
        ConfigHistory other = (ConfigHistory)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$configId = this.getConfigId();
        Long other$configId = other.getConfigId();
        if (this$configId == null ? other$configId != null : !((Object)this$configId).equals(other$configId)) {
            return false;
        }
        Integer this$version = this.getVersion();
        Integer other$version = other.getVersion();
        if (this$version == null ? other$version != null : !((Object)this$version).equals(other$version)) {
            return false;
        }
        Integer this$rollbackFrom = this.getRollbackFrom();
        Integer other$rollbackFrom = other.getRollbackFrom();
        if (this$rollbackFrom == null ? other$rollbackFrom != null : !((Object)this$rollbackFrom).equals(other$rollbackFrom)) {
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
        String this$operationType = this.getOperationType();
        String other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) {
            return false;
        }
        String this$operator = this.getOperator();
        String other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime)) {
            return false;
        }
        String this$md5 = this.getMd5();
        String other$md5 = other.getMd5();
        return !(this$md5 == null ? other$md5 != null : !this$md5.equals(other$md5));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ConfigHistory;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $configId = this.getConfigId();
        result = result * 59 + ($configId == null ? 43 : ((Object)$configId).hashCode());
        Integer $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : ((Object)$version).hashCode());
        Integer $rollbackFrom = this.getRollbackFrom();
        result = result * 59 + ($rollbackFrom == null ? 43 : ((Object)$rollbackFrom).hashCode());
        String $dataId = this.getDataId();
        result = result * 59 + ($dataId == null ? 43 : $dataId.hashCode());
        String $group = this.getGroup();
        result = result * 59 + ($group == null ? 43 : $group.hashCode());
        String $namespace = this.getNamespace();
        result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $operationType = this.getOperationType();
        result = result * 59 + ($operationType == null ? 43 : $operationType.hashCode());
        String $operator = this.getOperator();
        result = result * 59 + ($operator == null ? 43 : $operator.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        String $md5 = this.getMd5();
        result = result * 59 + ($md5 == null ? 43 : $md5.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ConfigHistory(id=" + this.getId() + ", configId=" + this.getConfigId() + ", dataId=" + this.getDataId() + ", group=" + this.getGroup() + ", namespace=" + this.getNamespace() + ", content=" + this.getContent() + ", version=" + this.getVersion() + ", operationType=" + this.getOperationType() + ", operator=" + this.getOperator() + ", rollbackFrom=" + this.getRollbackFrom() + ", createTime=" + String.valueOf(this.getCreateTime()) + ", md5=" + this.getMd5() + ")";
    }

    @Generated
    public ConfigHistory() {
    }

    @Generated
    public ConfigHistory(Long id, Long configId, String dataId, String group, String namespace, String content, Integer version, String operationType, String operator, Integer rollbackFrom, LocalDateTime createTime, String md5) {
        this.id = id;
        this.configId = configId;
        this.dataId = dataId;
        this.group = group;
        this.namespace = namespace;
        this.content = content;
        this.version = version;
        this.operationType = operationType;
        this.operator = operator;
        this.rollbackFrom = rollbackFrom;
        this.createTime = createTime;
        this.md5 = md5;
    }

    @Generated
    public static class ConfigHistoryBuilder {
        @Generated
        private Long id;
        @Generated
        private Long configId;
        @Generated
        private String dataId;
        @Generated
        private String group;
        @Generated
        private String namespace;
        @Generated
        private String content;
        @Generated
        private Integer version;
        @Generated
        private String operationType;
        @Generated
        private String operator;
        @Generated
        private Integer rollbackFrom;
        @Generated
        private LocalDateTime createTime;
        @Generated
        private String md5;

        @Generated
        ConfigHistoryBuilder() {
        }

        @Generated
        public ConfigHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder configId(Long configId) {
            this.configId = configId;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder dataId(String dataId) {
            this.dataId = dataId;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder group(String group) {
            this.group = group;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder content(String content) {
            this.content = content;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder version(Integer version) {
            this.version = version;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder operationType(String operationType) {
            this.operationType = operationType;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder operator(String operator) {
            this.operator = operator;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder rollbackFrom(Integer rollbackFrom) {
            this.rollbackFrom = rollbackFrom;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder createTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @Generated
        public ConfigHistoryBuilder md5(String md5) {
            this.md5 = md5;
            return this;
        }

        @Generated
        public ConfigHistory build() {
            return new ConfigHistory(this.id, this.configId, this.dataId, this.group, this.namespace, this.content, this.version, this.operationType, this.operator, this.rollbackFrom, this.createTime, this.md5);
        }

        @Generated
        public String toString() {
            return "ConfigHistory.ConfigHistoryBuilder(id=" + this.id + ", configId=" + this.configId + ", dataId=" + this.dataId + ", group=" + this.group + ", namespace=" + this.namespace + ", content=" + this.content + ", version=" + this.version + ", operationType=" + this.operationType + ", operator=" + this.operator + ", rollbackFrom=" + this.rollbackFrom + ", createTime=" + String.valueOf(this.createTime) + ", md5=" + this.md5 + ")";
        }
    }
}

