package com.basebackend.api.model.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务 DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record TaskFeignDTO(
        String id,
        String name,
        String localizedName,
        String description,
        String localizedDescription,
        String assignee,
        String owner,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime created,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime due,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime claimed,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime completed,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastUpdated,
        Integer priority,
        boolean isLast,
        String executionId,
        String processInstanceId,
        String processInstanceBusinessKey,
        String processDefinitionId,
        String processDefinitionKey,
        String processDefinitionName,
        Integer processDefinitionVersion,
        String activityId,
        String activityName,
        String activityInstanceId,
        String taskDefinitionKey,
        String tenantId,
        String formKey,
        String category,
        String parentTaskId,
        boolean suspended,
        boolean deleted,
        boolean ended,
        boolean claimable,
        String candidateUser,
        String candidateGroup,
        String involvedUser,
        String subProcessInstanceId,
        String superProcessInstanceId,
        Map<String, Object> variables
) implements Serializable {
}
