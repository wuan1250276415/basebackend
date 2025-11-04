package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流表单模板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 表单Key（唯一标识）
     */
    private String formKey;

    /**
     * 关联的流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 表单Schema JSON（JSON Schema格式）
     */
    private String schemaJson;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
