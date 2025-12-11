package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表单模板实体
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("camunda_form_template")
public class FormTemplateEntity {

    /**
     * 模板 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    @TableField("name")
    private String name;

    /**
     * 模板描述
     */
    @TableField("description")
    private String description;

    /**
     * 表单类型
     */
    @TableField("form_type")
    private String formType;

    /**
     * 表单内容
     */
    @TableField("content")
    private String content;

    /**
     * 表单模式定义
     */
    @TableField("schema")
    private String schema;

    /**
     * 租户 ID
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 版本号
     */
    @TableField("version")
    private Long version;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // 分页参数（非数据库字段）
    /**
     * 表单编码
     */
    @TableField("form_code")
    private String formCode;

    /**
     * 表单名称（别名）
     */
    @TableField("form_name")
    private String formName;

    /**
     * 流程定义Key
     */
    @TableField("process_definition_key")
    private String processDefinitionKey;

    /**
     * 表单Schema（别名，兼容性方法）
     */
    public String getFormSchema() {
        return this.schema;
    }

    /**
     * 表单Config（兼容性方法，暂无对应字段）
     */
    public String getFormConfig() {
        return null;
    }

    /**
     * 创建时间（别名，兼容性方法）
     */
    public LocalDateTime getCreateTime() {
        return this.createdAt;
    }

    /**
     * 更新时间（别名，兼容性方法）
     */
    public LocalDateTime getUpdateTime() {
        return this.updatedAt;
    }

    // 分页参数（非数据库字段）
    @TableField(exist = false)
    private Integer current;

    @TableField(exist = false)
    private Integer size;

    @TableField(exist = false)
    private String keyword;
}
