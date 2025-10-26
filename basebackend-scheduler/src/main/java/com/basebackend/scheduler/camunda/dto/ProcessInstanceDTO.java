package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 流程实例DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    private String id;

    /**
     * 业务键
     */
    private String businessKey;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 是否挂起
     */
    private Boolean suspended;

    /**
     * 是否结束
     */
    private Boolean ended;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 持续时间（毫秒）
     */
    private Long durationInMillis;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;
}
