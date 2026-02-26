package com.basebackend.api.model.scheduler;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程定义 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record ProcessDefinitionFeignDTO(
        String id,
        String key,
        String name,
        Integer version,
        String category,
        String deploymentId,
        boolean suspended,
        String description,
        String resourceName,
        String diagramResourceName,
        String startFormKey,
        String versionTag,
        String startableInTasklist,
        boolean startable,
        LocalDateTime created,
        String createdBy,
        LocalDateTime lastModified,
        String lastModifiedBy,
        String tenantId,
        String startActivityId,
        LocalDateTime deploymentTime
) implements Serializable {
}
