package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单模板分页查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "FormTemplatePageQuery", description = "表单模板分页查询请求")
public class FormTemplatePageQuery extends BasePageQuery {

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID，支持多租户过滤",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 表单类型
     */
    @Schema(
        description = "表单类型",
        example = "approval"
    )
    private String formType;

    /**
     * 状态
     */
    @Schema(
        description = "状态：ACTIVE=激活，DELETED=已删除",
        example = "ACTIVE"
    )
    private String status;

    /**
     * 关键词（名称、描述模糊搜索）
     */
    @Schema(
        description = "关键词，支持名称或描述模糊搜索",
        example = "审批"
    )
    private String keyword;

    /**
     * 表单编码
     */
    @Schema(
        description = "表单编码",
        example = "FORM_001"
    )
    private String formCode;

    /**
     * 表单名称
     */
    @Schema(
        description = "表单名称",
        example = "审批表单"
    )
    private String formName;

    /**
     * 流程定义Key
     */
    @Schema(
        description = "流程定义Key",
        example = "order_approval"
    )
    private String processDefinitionKey;

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
}
