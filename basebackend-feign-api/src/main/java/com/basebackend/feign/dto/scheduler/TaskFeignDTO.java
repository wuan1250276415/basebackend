package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record TaskFeignDTO(
        /** 任务ID */
        String id,

        /** 任务名称 */
        String name,

        /** 任务名称本地化变量 */
        String localizedName,

        /** 任务描述 */
        String description,

        /** 任务描述本地化变量 */
        String localizedDescription,

        /** 任务分配人 */
        String assignee,

        /** 任务拥有者 */
        String owner,

        /** 任务的创建时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime created,

        /** 任务的到期时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime due,

        /** 任务被声明的时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime claimed,

        /** 任务完成时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime completed,

        /** 任务的最后修改时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastUpdated,

        /** 优先级（默认：50） */
        Integer priority,

        /** 任务是否为查询结果中的最后一条 */
        boolean isLast,

        /** 任务的执行ID */
        String executionId,

        /** 任务所在的流程实例ID */
        String processInstanceId,

        /** 流程实例业务键 */
        String processInstanceBusinessKey,

        /** 任务所在的流程定义ID */
        String processDefinitionId,

        /** 流程定义键 */
        String processDefinitionKey,

        /** 流程定义名称 */
        String processDefinitionName,

        /** 流程定义版本 */
        Integer processDefinitionVersion,

        /** 任务所属活动（Service Task等）的ID */
        String activityId,

        /** 任务所属活动的名称 */
        String activityName,

        /** 任务所属活动实例ID */
        String activityInstanceId,

        /** 任务所在的任务列表的名称 */
        String taskDefinitionKey,

        /** 任务的候选用户组ID */
        String tenantId,

        /** 表单Key（用于启动流程和完成任务） */
        String formKey,

        /** 任务类别 */
        String category,

        /** 父任务ID */
        String parentTaskId,

        /** 任务是否Suspended（挂起） */
        boolean suspended,

        /** 任务是否已删除 */
        boolean deleted,

        /** 任务是否已结束 */
        boolean ended,

        /** 任务是否可被认领 */
        boolean claimable,

        /** 候选用户ID */
        String candidateUser,

        /** 候选组ID */
        String candidateGroup,

        /** 任务的处理人ID */
        String involvedUser,

        /** 任务的子实例数量（对于并行网关） */
        String subProcessInstanceId,

        /** 任务的父实例ID */
        String superProcessInstanceId,

        /** 任务变量 */
        Map<String, Object> variables
) implements Serializable {
}
