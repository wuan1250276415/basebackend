package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 表单模板更新请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "FormTemplateUpdateRequest", description = "表单模板更新请求参数")
public class FormTemplateUpdateRequest {

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
     * 状态
     */
    @Schema(
        description = "状态：ACTIVE=激活，INACTIVE=未激活，DELETED=已删除",
        example = "ACTIVE"
    )
    private String status;

    /**
     * 表单名称（别名，兼容性字段）
     */
    @Schema(
        description = "表单名称",
        example = "订单审批表单"
    )
    private String formName;

    /**
     * 获取表单Schema（兼容旧版本）
     */
    public String getFormSchema() {
        return schema;
    }

    /**
     * 获取表单Config（兼容旧版本，暂无字段）
     */
    public String getFormConfig() {
        return null;
    }

    // ========== 兼容性方法（用于测试兼容）==========

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
