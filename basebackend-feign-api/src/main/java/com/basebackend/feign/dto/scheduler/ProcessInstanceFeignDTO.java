package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record ProcessInstanceFeignDTO(
        /** 流程实例ID */
        String id,

        /** 流程实例业务键 */
        String businessKey,

        /** 流程定义ID */
        String processDefinitionId,

        /** 流程定义键 */
        String processDefinitionKey,

        /** 流程定义名称 */
        String processDefinitionName,

        /** 流程定义版本 */
        Integer processDefinitionVersion,

        /** 流程实例状态 */
        String state,

        /** 是否已结束 */
        boolean ended,

        /** 是否Suspended（挂起） */
        boolean suspended,

        /** 租户ID */
        String tenantId,

        /** 开始时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,

        /** 结束时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime,

        /** 开始活动ID */
        String startActivityId,

        /** 回调ID（用于通知已删除的实例） */
        String callbackId,

        /** 回调类型 */
        String callbackType,

        /** 引用（用于流程实例间引用） */
        String reference,

        /** 持续时间（毫秒） */
        Long durationInMillis,

        /** 创建时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime created,

        /** 流程变量 */
        Map<String, Object> variables
) implements Serializable {
}
