package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.basebackend.scheduler.camunda.entity.FormTemplateEntity;

import java.time.LocalDateTime;

/**
 * 表单模板数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FormTemplateDTO", description = "表单模板信息")
public class FormTemplateDTO {

    /**
     * 模板 ID
     */
    @Schema(description = "模板 ID", example = "1")
    private Long id;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称", example = "订单审批表单")
    private String name;

    /**
     * 模板描述
     */
    @Schema(description = "模板描述", example = "用于订单审批的标准表单")
    private String description;

    /**
     * 表单类型
     */
    @Schema(description = "表单类型", example = "approval")
    private String formType;

    /**
     * 表单内容
     */
    @Schema(description = "表单内容", example = "<form>...</form>")
    private String content;

    /**
     * 表单模式定义
     */
    @Schema(description = "表单模式定义", example = "{\"type\":\"object\"}")
    private String schema;

    /**
     * 租户 ID
     */
    @Schema(description = "租户 ID", example = "tenant_001")
    private String tenantId;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    /**
     * 版本号
     */
    @Schema(description = "版本号", example = "1")
    private Long version;

    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "admin")
    private String createdBy;

    /**
     * 更新人
     */
    @Schema(description = "更新人", example = "admin")
    private String updatedBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-01-01 10:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2025-01-01 10:00:00")
    private LocalDateTime updatedAt;

    /**
     * 表单编码
     */
    @Schema(description = "表单编码", example = "FORM_001")
    private String formCode;

    /**
     * 表单名称（别名）
     */
    @Schema(description = "表单名称", example = "订单审批表单")
    private String formName;

    /**
     * 流程定义Key
     */
    @Schema(description = "流程定义Key", example = "order_approval")
    private String processDefinitionKey;

    /**
     * 从实体转换为 DTO
     *
     * @param entity 表单模板实体
     * @return DTO 对象
     */
    public static FormTemplateDTO from(FormTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        return FormTemplateDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .formType(entity.getFormType())
                .content(entity.getContent())
                .schema(entity.getSchema())
                .tenantId(entity.getTenantId())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .formCode(entity.getFormCode())
                .formName(entity.getFormName())
                .processDefinitionKey(entity.getProcessDefinitionKey())
                .build();
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置模板类型（兼容性方法）
     * @param templateType 模板类型
     */
    public void setTemplateType(String templateType) {
        this.formType = templateType;
    }

    /**
     * 获取模板类型（兼容性方法）
     * @return 模板类型
     */
    public String getTemplateType() {
        return this.formType;
    }

    /**
     * 设置创建时间（兼容性方法）
     * @param created 创建时间
     */
    public void setCreated(java.time.Instant created) {
        this.createdAt = created == null ? null : LocalDateTime.ofInstant(created, java.time.ZoneId.systemDefault());
    }

    /**
     * 获取创建时间（兼容性方法）
     * @return 创建时间
     */
    public java.time.Instant getCreated() {
        return this.createdAt == null ? null : createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    /**
     * 设置更新时间（兼容性方法）
     * @param updated 更新时间
     */
    public void setUpdated(java.time.Instant updated) {
        this.updatedAt = updated == null ? null : LocalDateTime.ofInstant(updated, java.time.ZoneId.systemDefault());
    }

    /**
     * 获取更新时间（兼容性方法）
     * @return 更新时间
     */
    public java.time.Instant getUpdated() {
        return this.updatedAt == null ? null : updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    /**
     * 设置表单内容（兼容性方法）
     * @param formContent 表单内容
     */
    public void setFormContent(String formContent) {
        this.content = formContent;
    }

    /**
     * 获取表单内容（兼容性方法）
     * @return 表单内容
     */
    public String getFormContent() {
        return this.content;
    }
}
