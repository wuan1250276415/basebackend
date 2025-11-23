package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 表单模板 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class FormTemplateFeignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表单ID
     */
    private Long id;

    /**
     * 表单名称
     */
    private String name;

    /**
     * 表单描述
     */
    private String description;

    /**
     * 表单编码（唯一）
     */
    private String code;

    /**
     * 表单版本
     */
    private String version;

    /**
     * 表单状态
     */
    private String status;

    /**
     * 表单分类
     */
    private String category;

    /**
     * 表单内容（JSON格式）
     */
    private String content;

    /**
     * 表单样式
     */
    private String style;

    /**
     * 表单脚本
     */
    private String script;

    /**
     * 流程定义键（关联的工作流）
     */
    private String processDefinitionKey;

    /**
     * 任务定义键（关联的任务）
     */
    private String taskDefinitionKey;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 是否为系统预置表单
     */
    private boolean system;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 租户ID
     */
    private String tenantId;
}
