package com.basebackend.scheduler.camunda.dto;

/**
 * 流程实例终止请求 DTO
 */
public record TerminateRequest(
        String processInstanceId,
        String reason
) {
}
