package com.basebackend.scheduler.camunda.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务DTO
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class TaskDTO {

    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务Key
     */
    private String key;

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
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 任务认领时间
     */
    private LocalDateTime claimTime;

    /**
     * 截止日期
     */
    private LocalDateTime dueDate;

    /**
     * 跟进日期
     */
    private LocalDateTime followUpDate;

    /**
     * 任务优先级
     */
    private Integer priority;

    /**
     * 任务所属者
     */
    private String owner;

    /**
     * 任务受理人
     */
    private String assignee;

    /**
     * 任务类别
     */
    private String category;

    /**
     * 表单Key
     */
    private String formKey;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 是否已挂起
     */
    private Boolean suspended;

    /**
     * 任务变量
     */
    private Map<String, Object> variables;

    /**
     * 任务活动ID
     */
    private String activityId;

    /**
     * 任务活动实例ID
     */
    private String activityInstanceId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 任务状态枚举
     */
    public enum Status {
        CREATED,     // 已创建
        ASSIGNED,    // 已分配
        COMPLETED,   // 已完成
        CANCELLED,   // 已取消
        SUSPENDED    // 已挂起
    }
}
