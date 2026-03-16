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

public class GrayReleaseHistory
implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long grayConfigId;
    private String dataId;
    private String group;
    private String namespace;
    private String strategyType;
    private Integer percentage;
    private String labels;
    private String targetInstances;
    private Integer effectiveInstanceCount;
    private String operationType;
    private String grayContent;
    private String originalContent;
    private String operator;
    private LocalDateTime operationTime;
    private String result;
    private String failureReason;
    private String remark;
    private LocalDateTime createTime;

    @Generated
    public static GrayReleaseHistoryBuilder builder() {
        return new GrayReleaseHistoryBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public Long getGrayConfigId() {
        return this.grayConfigId;
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
    public String getStrategyType() {
        return this.strategyType;
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
    public String getTargetInstances() {
        return this.targetInstances;
    }

    @Generated
    public Integer getEffectiveInstanceCount() {
        return this.effectiveInstanceCount;
    }

    @Generated
    public String getOperationType() {
        return this.operationType;
    }

    @Generated
    public String getGrayContent() {
        return this.grayContent;
    }

    @Generated
    public String getOriginalContent() {
        return this.originalContent;
    }

    @Generated
    public String getOperator() {
        return this.operator;
    }

    @Generated
    public LocalDateTime getOperationTime() {
        return this.operationTime;
    }

    @Generated
    public String getResult() {
        return this.result;
    }

    @Generated
    public String getFailureReason() {
        return this.failureReason;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setGrayConfigId(Long grayConfigId) {
        this.grayConfigId = grayConfigId;
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
    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
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
    public void setTargetInstances(String targetInstances) {
        this.targetInstances = targetInstances;
    }

    @Generated
    public void setEffectiveInstanceCount(Integer effectiveInstanceCount) {
        this.effectiveInstanceCount = effectiveInstanceCount;
    }

    @Generated
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    @Generated
    public void setGrayContent(String grayContent) {
        this.grayContent = grayContent;
    }

    @Generated
    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    @Generated
    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Generated
    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    @Generated
    public void setResult(String result) {
        this.result = result;
    }

    @Generated
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GrayReleaseHistory)) {
            return false;
        }
        GrayReleaseHistory other = (GrayReleaseHistory)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$grayConfigId = this.getGrayConfigId();
        Long other$grayConfigId = other.getGrayConfigId();
        if (this$grayConfigId == null ? other$grayConfigId != null : !((Object)this$grayConfigId).equals(other$grayConfigId)) {
            return false;
        }
        Integer this$percentage = this.getPercentage();
        Integer other$percentage = other.getPercentage();
        if (this$percentage == null ? other$percentage != null : !((Object)this$percentage).equals(other$percentage)) {
            return false;
        }
        Integer this$effectiveInstanceCount = this.getEffectiveInstanceCount();
        Integer other$effectiveInstanceCount = other.getEffectiveInstanceCount();
        if (this$effectiveInstanceCount == null ? other$effectiveInstanceCount != null : !((Object)this$effectiveInstanceCount).equals(other$effectiveInstanceCount)) {
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
        String this$strategyType = this.getStrategyType();
        String other$strategyType = other.getStrategyType();
        if (this$strategyType == null ? other$strategyType != null : !this$strategyType.equals(other$strategyType)) {
            return false;
        }
        String this$labels = this.getLabels();
        String other$labels = other.getLabels();
        if (this$labels == null ? other$labels != null : !this$labels.equals(other$labels)) {
            return false;
        }
        String this$targetInstances = this.getTargetInstances();
        String other$targetInstances = other.getTargetInstances();
        if (this$targetInstances == null ? other$targetInstances != null : !this$targetInstances.equals(other$targetInstances)) {
            return false;
        }
        String this$operationType = this.getOperationType();
        String other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) {
            return false;
        }
        String this$grayContent = this.getGrayContent();
        String other$grayContent = other.getGrayContent();
        if (this$grayContent == null ? other$grayContent != null : !this$grayContent.equals(other$grayContent)) {
            return false;
        }
        String this$originalContent = this.getOriginalContent();
        String other$originalContent = other.getOriginalContent();
        if (this$originalContent == null ? other$originalContent != null : !this$originalContent.equals(other$originalContent)) {
            return false;
        }
        String this$operator = this.getOperator();
        String other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) {
            return false;
        }
        LocalDateTime this$operationTime = this.getOperationTime();
        LocalDateTime other$operationTime = other.getOperationTime();
        if (this$operationTime == null ? other$operationTime != null : !((Object)this$operationTime).equals(other$operationTime)) {
            return false;
        }
        String this$result = this.getResult();
        String other$result = other.getResult();
        if (this$result == null ? other$result != null : !this$result.equals(other$result)) {
            return false;
        }
        String this$failureReason = this.getFailureReason();
        String other$failureReason = other.getFailureReason();
        if (this$failureReason == null ? other$failureReason != null : !this$failureReason.equals(other$failureReason)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        return !(this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof GrayReleaseHistory;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $grayConfigId = this.getGrayConfigId();
        result = result * 59 + ($grayConfigId == null ? 43 : ((Object)$grayConfigId).hashCode());
        Integer $percentage = this.getPercentage();
        result = result * 59 + ($percentage == null ? 43 : ((Object)$percentage).hashCode());
        Integer $effectiveInstanceCount = this.getEffectiveInstanceCount();
        result = result * 59 + ($effectiveInstanceCount == null ? 43 : ((Object)$effectiveInstanceCount).hashCode());
        String $dataId = this.getDataId();
        result = result * 59 + ($dataId == null ? 43 : $dataId.hashCode());
        String $group = this.getGroup();
        result = result * 59 + ($group == null ? 43 : $group.hashCode());
        String $namespace = this.getNamespace();
        result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
        String $strategyType = this.getStrategyType();
        result = result * 59 + ($strategyType == null ? 43 : $strategyType.hashCode());
        String $labels = this.getLabels();
        result = result * 59 + ($labels == null ? 43 : $labels.hashCode());
        String $targetInstances = this.getTargetInstances();
        result = result * 59 + ($targetInstances == null ? 43 : $targetInstances.hashCode());
        String $operationType = this.getOperationType();
        result = result * 59 + ($operationType == null ? 43 : $operationType.hashCode());
        String $grayContent = this.getGrayContent();
        result = result * 59 + ($grayContent == null ? 43 : $grayContent.hashCode());
        String $originalContent = this.getOriginalContent();
        result = result * 59 + ($originalContent == null ? 43 : $originalContent.hashCode());
        String $operator = this.getOperator();
        result = result * 59 + ($operator == null ? 43 : $operator.hashCode());
        LocalDateTime $operationTime = this.getOperationTime();
        result = result * 59 + ($operationTime == null ? 43 : ((Object)$operationTime).hashCode());
        String $result = this.getResult();
        result = result * 59 + ($result == null ? 43 : $result.hashCode());
        String $failureReason = this.getFailureReason();
        result = result * 59 + ($failureReason == null ? 43 : $failureReason.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "GrayReleaseHistory(id=" + this.getId() + ", grayConfigId=" + this.getGrayConfigId() + ", dataId=" + this.getDataId() + ", group=" + this.getGroup() + ", namespace=" + this.getNamespace() + ", strategyType=" + this.getStrategyType() + ", percentage=" + this.getPercentage() + ", labels=" + this.getLabels() + ", targetInstances=" + this.getTargetInstances() + ", effectiveInstanceCount=" + this.getEffectiveInstanceCount() + ", operationType=" + this.getOperationType() + ", grayContent=" + this.getGrayContent() + ", originalContent=" + this.getOriginalContent() + ", operator=" + this.getOperator() + ", operationTime=" + String.valueOf(this.getOperationTime()) + ", result=" + this.getResult() + ", failureReason=" + this.getFailureReason() + ", remark=" + this.getRemark() + ", createTime=" + String.valueOf(this.getCreateTime()) + ")";
    }

    @Generated
    public GrayReleaseHistory() {
    }

    @Generated
    public GrayReleaseHistory(Long id, Long grayConfigId, String dataId, String group, String namespace, String strategyType, Integer percentage, String labels, String targetInstances, Integer effectiveInstanceCount, String operationType, String grayContent, String originalContent, String operator, LocalDateTime operationTime, String result, String failureReason, String remark, LocalDateTime createTime) {
        this.id = id;
        this.grayConfigId = grayConfigId;
        this.dataId = dataId;
        this.group = group;
        this.namespace = namespace;
        this.strategyType = strategyType;
        this.percentage = percentage;
        this.labels = labels;
        this.targetInstances = targetInstances;
        this.effectiveInstanceCount = effectiveInstanceCount;
        this.operationType = operationType;
        this.grayContent = grayContent;
        this.originalContent = originalContent;
        this.operator = operator;
        this.operationTime = operationTime;
        this.result = result;
        this.failureReason = failureReason;
        this.remark = remark;
        this.createTime = createTime;
    }

    @Generated
    public static class GrayReleaseHistoryBuilder {
        @Generated
        private Long id;
        @Generated
        private Long grayConfigId;
        @Generated
        private String dataId;
        @Generated
        private String group;
        @Generated
        private String namespace;
        @Generated
        private String strategyType;
        @Generated
        private Integer percentage;
        @Generated
        private String labels;
        @Generated
        private String targetInstances;
        @Generated
        private Integer effectiveInstanceCount;
        @Generated
        private String operationType;
        @Generated
        private String grayContent;
        @Generated
        private String originalContent;
        @Generated
        private String operator;
        @Generated
        private LocalDateTime operationTime;
        @Generated
        private String result;
        @Generated
        private String failureReason;
        @Generated
        private String remark;
        @Generated
        private LocalDateTime createTime;

        @Generated
        GrayReleaseHistoryBuilder() {
        }

        @Generated
        public GrayReleaseHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder grayConfigId(Long grayConfigId) {
            this.grayConfigId = grayConfigId;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder dataId(String dataId) {
            this.dataId = dataId;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder group(String group) {
            this.group = group;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder strategyType(String strategyType) {
            this.strategyType = strategyType;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder percentage(Integer percentage) {
            this.percentage = percentage;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder labels(String labels) {
            this.labels = labels;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder targetInstances(String targetInstances) {
            this.targetInstances = targetInstances;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder effectiveInstanceCount(Integer effectiveInstanceCount) {
            this.effectiveInstanceCount = effectiveInstanceCount;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder operationType(String operationType) {
            this.operationType = operationType;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder grayContent(String grayContent) {
            this.grayContent = grayContent;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder originalContent(String originalContent) {
            this.originalContent = originalContent;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder operator(String operator) {
            this.operator = operator;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder operationTime(LocalDateTime operationTime) {
            this.operationTime = operationTime;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder result(String result) {
            this.result = result;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder remark(String remark) {
            this.remark = remark;
            return this;
        }

        @Generated
        public GrayReleaseHistoryBuilder createTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @Generated
        public GrayReleaseHistory build() {
            return new GrayReleaseHistory(this.id, this.grayConfigId, this.dataId, this.group, this.namespace, this.strategyType, this.percentage, this.labels, this.targetInstances, this.effectiveInstanceCount, this.operationType, this.grayContent, this.originalContent, this.operator, this.operationTime, this.result, this.failureReason, this.remark, this.createTime);
        }

        @Generated
        public String toString() {
            return "GrayReleaseHistory.GrayReleaseHistoryBuilder(id=" + this.id + ", grayConfigId=" + this.grayConfigId + ", dataId=" + this.dataId + ", group=" + this.group + ", namespace=" + this.namespace + ", strategyType=" + this.strategyType + ", percentage=" + this.percentage + ", labels=" + this.labels + ", targetInstances=" + this.targetInstances + ", effectiveInstanceCount=" + this.effectiveInstanceCount + ", operationType=" + this.operationType + ", grayContent=" + this.grayContent + ", originalContent=" + this.originalContent + ", operator=" + this.operator + ", operationTime=" + String.valueOf(this.operationTime) + ", result=" + this.result + ", failureReason=" + this.failureReason + ", remark=" + this.remark + ", createTime=" + String.valueOf(this.createTime) + ")";
        }
    }

    public static enum OperationResult {
        SUCCESS("SUCCESS", "\u6210\u529f"),
        FAILED("FAILED", "\u5931\u8d25");

        private final String code;
        private final String description;

        private OperationResult(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return this.code;
        }

        public String getDescription() {
            return this.description;
        }
    }

    public static enum OperationType {
        START("START", "\u5f00\u59cb\u7070\u5ea6\u53d1\u5e03"),
        PROMOTE("PROMOTE", "\u5168\u91cf\u53d1\u5e03"),
        ROLLBACK("ROLLBACK", "\u56de\u6eda");

        private final String code;
        private final String description;

        private OperationType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return this.code;
        }

        public String getDescription() {
            return this.description;
        }
    }
}

