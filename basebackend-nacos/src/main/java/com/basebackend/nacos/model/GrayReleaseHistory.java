package com.basebackend.nacos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 灰度发布历史记录模型
 * <p>
 * 记录每次灰度发布的详细信息，用于审计和回溯
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrayReleaseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 关联的灰度配置ID
     */
    private Long grayConfigId;

    /**
     * 配置Data ID
     */
    private String dataId;

    /**
     * 配置分组
     */
    private String group;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 灰度策略类型（ip/percentage/label）
     */
    private String strategyType;

    /**
     * 灰度百分比（百分比策略时使用）
     */
    private Integer percentage;

    /**
     * 实例标签（标签策略时使用，JSON格式）
     */
    private String labels;

    /**
     * 目标实例列表（逗号分隔）
     */
    private String targetInstances;

    /**
     * 实际生效实例数量
     */
    private Integer effectiveInstanceCount;

    /**
     * 操作类型（START/PROMOTE/ROLLBACK）
     */
    private String operationType;

    /**
     * 灰度配置内容
     */
    private String grayContent;

    /**
     * 原始配置内容（用于回滚）
     */
    private String originalContent;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作结果（SUCCESS/FAILED）
     */
    private String result;

    /**
     * 失败原因（操作失败时记录）
     */
    private String failureReason;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        /**
         * 开始灰度发布
         */
        START("START", "开始灰度发布"),
        /**
         * 全量发布
         */
        PROMOTE("PROMOTE", "全量发布"),
        /**
         * 回滚
         */
        ROLLBACK("ROLLBACK", "回滚");

        private final String code;
        private final String description;

        OperationType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS("SUCCESS", "成功"),
        FAILED("FAILED", "失败");

        private final String code;
        private final String description;

        OperationResult(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
