package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class ProcessInstanceFeignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    private String id;

    /**
     * 流程实例业务键
     */
    private String businessKey;

    /**
     * 流程定义ID
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
     * 流程实例状态
     */
    private String state;

    /**
     * 是否已结束
     */
    private boolean ended;

    /**
     * 是否Suspended（挂起）
     */
    private boolean suspended;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 开始活动ID
     */
    private String startActivityId;

    /**
     * 回调ID（用于通知已删除的实例）
     */
    private String callbackId;

    /**
     * 回调类型
     */
    private String callbackType;

    /**
     * 引用（用于流程实例间引用）
     */
    private String reference;

    /**
     * 持续时间（毫秒）
     */
    private Long durationInMillis;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;
}
