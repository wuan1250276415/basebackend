package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 历史流程实例DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricProcessInstanceDTO implements Serializable {

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
     * 流程定义版本
     */
    private Integer processDefinitionVersion;

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
     * 开始活动ID
     */
    private String startActivityId;

    /**
     * 结束活动ID
     */
    private String endActivityId;

    /**
     * 启动用户ID
     */
    private String startUserId;

    /**
     * 删除原因
     */
    private String deleteReason;

    /**
     * 状态（ACTIVE, COMPLETED, EXTERNALLY_TERMINATED, INTERNALLY_TERMINATED）
     */
    private String state;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;
}
