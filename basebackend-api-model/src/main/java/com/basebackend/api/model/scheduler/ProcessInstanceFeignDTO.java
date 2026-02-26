package com.basebackend.api.model.scheduler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record ProcessInstanceFeignDTO(
        String id,
        String businessKey,
        String processDefinitionId,
        String processDefinitionKey,
        String processDefinitionName,
        Integer processDefinitionVersion,
        String state,
        boolean ended,
        boolean suspended,
        String tenantId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String startActivityId,
        String callbackId,
        String callbackType,
        String reference,
        Long durationInMillis,
        LocalDateTime created,
        Map<String, Object> variables
) implements Serializable {
}
