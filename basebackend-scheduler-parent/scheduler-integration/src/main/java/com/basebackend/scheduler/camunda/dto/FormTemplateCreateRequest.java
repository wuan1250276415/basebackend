package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 表单模板创建请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "FormTemplateCreateRequest", description = "表单模板创建请求参数")
public class FormTemplateCreateRequest {

    /**
     * 模板名称
     */
    @Schema(
        description = "模板名称",
        example = "订单审批表单",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /**
     * 模板描述
     */
    @Schema(
        description = "模板描述",
        example = "用于订单审批的标准表单"
    )
    private String description;

    /**
     * 表单类型
     */
    @Schema(
        description = "表单类型",
        example = "approval",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "表单类型不能为空")
    private String formType;

    /**
     * 表单内容（HTML、JSON 或其他格式）
     */
    @Schema(
        description = "表单内容（HTML、JSON 或其他格式）",
        example = "<form>...</form>",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "表单内容不能为空")
    private String content;

    /**
     * 表单模式定义（JSON Schema 或其他）
     */
    @Schema(
        description = "表单模式定义（JSON Schema 或其他）",
        example = "{\"type\":\"object\",\"properties\":{...}}"
    )
    private String schema;

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID",
        example = "tenant_001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "租户 ID 不能为空")
    private String tenantId;

    /**
     * 表单编码
     */
    @Schema(
        description = "表单编码",
        example = "FORM_001"
    )
    private String formCode;

    /**
     * 表单名称（别名）
     */
    @Schema(
        description = "表单名称",
        example = "订单审批表单"
    )
    private String formName;

    /**
     * 表单配置
     */
    @Schema(
        description = "表单配置（JSON格式）",
        example = "{\"layout\":\"vertical\"}"
    )
    private String formConfig;

    /**
     * 流程定义Key
     */
    @Schema(
        description = "流程定义Key",
        example = "order_approval"
    )
    private String processDefinitionKey;

    /**
     * 表单模式（别名）
     */
    @Schema(
        description = "表单模式定义",
        example = "{\"type\":\"object\"}"
    )
    private String formSchema;

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
