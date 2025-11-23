package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class TaskFeignDTO implements Serializable {

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
     * 任务名称本地化变量
     */
    private String localizedName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务描述本地化变量
     */
    private String localizedDescription;

    /**
     * 任务分配人
     */
    private String assignee;

    /**
     * 任务拥有者
     */
    private String owner;

    /**
     * 任务的创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    /**
     * 任务的到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime due;

    /**
     * 任务被声明的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime claimed;

    /**
     * 任务完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completed;

    /**
     * 任务的最后修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;

    /**
     * 优先级（默认：50）
     */
    private Integer priority;

    /**
     * 任务是否为查询结果中的最后一条
     */
    private boolean isLast;

    /**
     * 任务的执行ID
     */
    private String executionId;

    /**
     * 任务所在的流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程实例业务键
     */
    private String processInstanceBusinessKey;

    /**
     * 任务所在的流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义键
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
     * 任务所属活动（Service Task等）的ID
     */
    private String activityId;

    /**
     * 任务所属活动的名称
     */
    private String activityName;

    /**
     * 任务所属活动实例ID
     */
    private String activityInstanceId;

    /**
     * 任务所在的任务列表的名称
     */
    private String taskDefinitionKey;

    /**
     * 任务的候选用户组ID
     */
    private String tenantId;

    /**
     * 表单Key（用于启动流程和完成任务）
     */
    private String formKey;

    /**
     * 任务类别
     */
    private String category;

    /**
     * 父任务ID
     */
    private String parentTaskId;

    /**
     * 任务是否Suspended（挂起）
     */
    private boolean suspended;

    /**
     * 任务是否已删除
     */
    private boolean deleted;

    /**
     * 任务是否已结束
     */
    private boolean ended;

    /**
     * 任务是否可被认领
     */
    private boolean claimable;

    /**
     * 候选用户ID
     */
    private String candidateUser;

    /**
     * 候选组ID
     */
    private String candidateGroup;

    /**
     * 任务的处理人ID
     */
    private String involvedUser;

    /**
     * 任务的子实例数量（对于并行网关）
     */
    private String subProcessInstanceId;

    /**
     * 任务的父实例ID
     */
    private String superProcessInstanceId;

    /**
     * 任务变量
     */
    private Map<String, Object> variables;
}
