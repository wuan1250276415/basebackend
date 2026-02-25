package com.basebackend.api.model.scheduler;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 表单模板 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record FormTemplateFeignDTO(
        Long id,
        String name,
        String description,
        String code,
        String version,
        String status,
        String category,
        String content,
        String style,
        String script,
        String processDefinitionKey,
        String taskDefinitionKey,
        String businessType,
        boolean system,
        Integer sortOrder,
        boolean enabled,
        LocalDateTime createdTime,
        String createdBy,
        LocalDateTime updatedTime,
        String updatedBy,
        String tenantId
) implements Serializable {
}
