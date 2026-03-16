/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Generated;

public class GrayReleaseConfig
implements Serializable {
    private Long id;
    private Long configId;
    private String dataId;
    private String strategyType;
    private String targetInstances;
    private Integer percentage;
    private String labels;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime promoteTime;
    private LocalDateTime rollbackTime;
    private String grayContent;
    private transient List<String> effectiveInstances;

    @Generated
    public static GrayReleaseConfigBuilder builder() {
        return new GrayReleaseConfigBuilder();
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
    public String getStrategyType() {
        return this.strategyType;
    }

    @Generated
    public String getTargetInstances() {
        return this.targetInstances;
    }

    @Generated
    public Integer getPercentage() {
        return this.percentage;
    }

    @Generated
    public String getLabels() {
        return this.labels;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    @Generated
    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    @Generated
    public LocalDateTime getPromoteTime() {
        return this.promoteTime;
    }

    @Generated
    public LocalDateTime getRollbackTime() {
        return this.rollbackTime;
    }

    @Generated
    public String getGrayContent() {
        return this.grayContent;
    }

    @Generated
    public List<String> getEffectiveInstances() {
        return this.effectiveInstances;
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
    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    @Generated
    public void setTargetInstances(String targetInstances) {
        this.targetInstances = targetInstances;
    }

    @Generated
    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    @Generated
    public void setLabels(String labels) {
        this.labels = labels;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @Generated
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Generated
    public void setPromoteTime(LocalDateTime promoteTime) {
        this.promoteTime = promoteTime;
    }

    @Generated
    public void setRollbackTime(LocalDateTime rollbackTime) {
        this.rollbackTime = rollbackTime;
    }

    @Generated
    public void setGrayContent(String grayContent) {
        this.grayContent = grayContent;
    }

    @Generated
    public void setEffectiveInstances(List<String> effectiveInstances) {
        this.effectiveInstances = effectiveInstances;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GrayReleaseConfig)) {
            return false;
        }
        GrayReleaseConfig other = (GrayReleaseConfig)o;
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
        Integer this$percentage = this.getPercentage();
        Integer other$percentage = other.getPercentage();
        if (this$percentage == null ? other$percentage != null : !((Object)this$percentage).equals(other$percentage)) {
            return false;
        }
        String this$dataId = this.getDataId();
        String other$dataId = other.getDataId();
        if (this$dataId == null ? other$dataId != null : !this$dataId.equals(other$dataId)) {
            return false;
        }
        String this$strategyType = this.getStrategyType();
        String other$strategyType = other.getStrategyType();
        if (this$strategyType == null ? other$strategyType != null : !this$strategyType.equals(other$strategyType)) {
            return false;
        }
        String this$targetInstances = this.getTargetInstances();
        String other$targetInstances = other.getTargetInstances();
        if (this$targetInstances == null ? other$targetInstances != null : !this$targetInstances.equals(other$targetInstances)) {
            return false;
        }
        String this$labels = this.getLabels();
        String other$labels = other.getLabels();
        if (this$labels == null ? other$labels != null : !this$labels.equals(other$labels)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        LocalDateTime this$startTime = this.getStartTime();
        LocalDateTime other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !((Object)this$startTime).equals(other$startTime)) {
            return false;
        }
        LocalDateTime this$endTime = this.getEndTime();
        LocalDateTime other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !((Object)this$endTime).equals(other$endTime)) {
            return false;
        }
        LocalDateTime this$promoteTime = this.getPromoteTime();
        LocalDateTime other$promoteTime = other.getPromoteTime();
        if (this$promoteTime == null ? other$promoteTime != null : !((Object)this$promoteTime).equals(other$promoteTime)) {
            return false;
        }
        LocalDateTime this$rollbackTime = this.getRollbackTime();
        LocalDateTime other$rollbackTime = other.getRollbackTime();
        if (this$rollbackTime == null ? other$rollbackTime != null : !((Object)this$rollbackTime).equals(other$rollbackTime)) {
            return false;
        }
        String this$grayContent = this.getGrayContent();
        String other$grayContent = other.getGrayContent();
        return !(this$grayContent == null ? other$grayContent != null : !this$grayContent.equals(other$grayContent));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof GrayReleaseConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $configId = this.getConfigId();
        result = result * 59 + ($configId == null ? 43 : ((Object)$configId).hashCode());
        Integer $percentage = this.getPercentage();
        result = result * 59 + ($percentage == null ? 43 : ((Object)$percentage).hashCode());
        String $dataId = this.getDataId();
        result = result * 59 + ($dataId == null ? 43 : $dataId.hashCode());
        String $strategyType = this.getStrategyType();
        result = result * 59 + ($strategyType == null ? 43 : $strategyType.hashCode());
        String $targetInstances = this.getTargetInstances();
        result = result * 59 + ($targetInstances == null ? 43 : $targetInstances.hashCode());
        String $labels = this.getLabels();
        result = result * 59 + ($labels == null ? 43 : $labels.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        LocalDateTime $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : ((Object)$startTime).hashCode());
        LocalDateTime $endTime = this.getEndTime();
        result = result * 59 + ($endTime == null ? 43 : ((Object)$endTime).hashCode());
        LocalDateTime $promoteTime = this.getPromoteTime();
        result = result * 59 + ($promoteTime == null ? 43 : ((Object)$promoteTime).hashCode());
        LocalDateTime $rollbackTime = this.getRollbackTime();
        result = result * 59 + ($rollbackTime == null ? 43 : ((Object)$rollbackTime).hashCode());
        String $grayContent = this.getGrayContent();
        result = result * 59 + ($grayContent == null ? 43 : $grayContent.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "GrayReleaseConfig(id=" + this.getId() + ", configId=" + this.getConfigId() + ", dataId=" + this.getDataId() + ", strategyType=" + this.getStrategyType() + ", targetInstances=" + this.getTargetInstances() + ", percentage=" + this.getPercentage() + ", labels=" + this.getLabels() + ", status=" + this.getStatus() + ", startTime=" + String.valueOf(this.getStartTime()) + ", endTime=" + String.valueOf(this.getEndTime()) + ", promoteTime=" + String.valueOf(this.getPromoteTime()) + ", rollbackTime=" + String.valueOf(this.getRollbackTime()) + ", grayContent=" + this.getGrayContent() + ", effectiveInstances=" + String.valueOf(this.getEffectiveInstances()) + ")";
    }

    @Generated
    public GrayReleaseConfig() {
    }

    @Generated
    public GrayReleaseConfig(Long id, Long configId, String dataId, String strategyType, String targetInstances, Integer percentage, String labels, String status, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime promoteTime, LocalDateTime rollbackTime, String grayContent, List<String> effectiveInstances) {
        this.id = id;
        this.configId = configId;
        this.dataId = dataId;
        this.strategyType = strategyType;
        this.targetInstances = targetInstances;
        this.percentage = percentage;
        this.labels = labels;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.promoteTime = promoteTime;
        this.rollbackTime = rollbackTime;
        this.grayContent = grayContent;
        this.effectiveInstances = effectiveInstances;
    }

    @Generated
    public static class GrayReleaseConfigBuilder {
        @Generated
        private Long id;
        @Generated
        private Long configId;
        @Generated
        private String dataId;
        @Generated
        private String strategyType;
        @Generated
        private String targetInstances;
        @Generated
        private Integer percentage;
        @Generated
        private String labels;
        @Generated
        private String status;
        @Generated
        private LocalDateTime startTime;
        @Generated
        private LocalDateTime endTime;
        @Generated
        private LocalDateTime promoteTime;
        @Generated
        private LocalDateTime rollbackTime;
        @Generated
        private String grayContent;
        @Generated
        private List<String> effectiveInstances;

        @Generated
        GrayReleaseConfigBuilder() {
        }

        @Generated
        public GrayReleaseConfigBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder configId(Long configId) {
            this.configId = configId;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder dataId(String dataId) {
            this.dataId = dataId;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder strategyType(String strategyType) {
            this.strategyType = strategyType;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder targetInstances(String targetInstances) {
            this.targetInstances = targetInstances;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder percentage(Integer percentage) {
            this.percentage = percentage;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder labels(String labels) {
            this.labels = labels;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder status(String status) {
            this.status = status;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder promoteTime(LocalDateTime promoteTime) {
            this.promoteTime = promoteTime;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder rollbackTime(LocalDateTime rollbackTime) {
            this.rollbackTime = rollbackTime;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder grayContent(String grayContent) {
            this.grayContent = grayContent;
            return this;
        }

        @Generated
        public GrayReleaseConfigBuilder effectiveInstances(List<String> effectiveInstances) {
            this.effectiveInstances = effectiveInstances;
            return this;
        }

        @Generated
        public GrayReleaseConfig build() {
            return new GrayReleaseConfig(this.id, this.configId, this.dataId, this.strategyType, this.targetInstances, this.percentage, this.labels, this.status, this.startTime, this.endTime, this.promoteTime, this.rollbackTime, this.grayContent, this.effectiveInstances);
        }

        @Generated
        public String toString() {
            return "GrayReleaseConfig.GrayReleaseConfigBuilder(id=" + this.id + ", configId=" + this.configId + ", dataId=" + this.dataId + ", strategyType=" + this.strategyType + ", targetInstances=" + this.targetInstances + ", percentage=" + this.percentage + ", labels=" + this.labels + ", status=" + this.status + ", startTime=" + String.valueOf(this.startTime) + ", endTime=" + String.valueOf(this.endTime) + ", promoteTime=" + String.valueOf(this.promoteTime) + ", rollbackTime=" + String.valueOf(this.rollbackTime) + ", grayContent=" + this.grayContent + ", effectiveInstances=" + String.valueOf(this.effectiveInstances) + ")";
        }
    }
}

