package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程实例终止请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminateRequest {

    /**
     * 流程实例 ID
     */
    private String processInstanceId;

    /**
     * 终止原因
     */
    private String reason;
}
