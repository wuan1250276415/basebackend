package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务定义Key
     */
    private String taskDefinitionKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 办理人
     */
    private String assignee;

    /**
     * 候选用户列表
     */
    private String candidateUsers;

    /**
     * 候选组列表
     */
    private String candidateGroups;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 到期时间
     */
    private Date dueDate;

    /**
     * 跟进时间
     */
    private Date followUpDate;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 描述
     */
    private String description;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 任务变量
     */
    private Map<String, Object> variables;
}
