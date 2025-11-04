package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流表单模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_form_template")
public class FormTemplateEntity extends BaseEntity {

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
     * 表单Key（唯一标识）
     */
    @TableField("form_key")
    private String formKey;

    /**
     * 关联的流程定义Key
     */
    @TableField("process_definition_key")
    private String processDefinitionKey;

    /**
     * 表单Schema JSON（JSON Schema格式）
     */
    @TableField("schema_json")
    private String schemaJson;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;
}
