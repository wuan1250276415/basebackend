package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 历史活动实例DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricActivityInstanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动实例ID
     */
    private String id;

    /**
     * 活动ID
     */
    private String activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动类型（userTask, serviceTask, startEvent, endEvent等）
     */
    private String activityType;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 办理人
     */
    private String assignee;

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
     * 删除原因
     */
    private String deleteReason;

    /**
     * 租户ID
     */
    private String tenantId;
}
