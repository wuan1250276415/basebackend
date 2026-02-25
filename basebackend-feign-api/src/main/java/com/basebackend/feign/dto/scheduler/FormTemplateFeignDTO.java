package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 表单模板 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record FormTemplateFeignDTO(
        /** 表单ID */
        Long id,

        /** 表单名称 */
        String name,

        /** 表单描述 */
        String description,

        /** 表单编码（唯一） */
        String code,

        /** 表单版本 */
        String version,

        /** 表单状态 */
        String status,

        /** 表单分类 */
        String category,

        /** 表单内容（JSON格式） */
        String content,

        /** 表单样式 */
        String style,

        /** 表单脚本 */
        String script,

        /** 流程定义键（关联的工作流） */
        String processDefinitionKey,

        /** 任务定义键（关联的任务） */
        String taskDefinitionKey,

        /** 业务类型 */
        String businessType,

        /** 是否为系统预置表单 */
        boolean system,

        /** 排序号 */
        Integer sortOrder,

        /** 是否启用 */
        boolean enabled,

        /** 创建时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdTime,

        /** 创建人 */
        String createdBy,

        /** 更新时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedTime,

        /** 更新人 */
        String updatedBy,

        /** 租户ID */
        String tenantId
) implements Serializable {
}
