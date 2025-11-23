package com.basebackend.scheduler.camunda.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程定义详情DTO
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class ProcessDefinitionDetailDTO {

    /**
     * 流程定义ID
     */
    private String id;

    /**
     * 流程定义Key
     */
    private String key;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 分类
     */
    private String category;

    /**
     * 描述
     */
    private String description;

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 图片资源名称
     */
    private String diagramResourceName;

    /**
     * 部署时间
     */
    private LocalDateTime deploymentTime;

    /**
     * 是否uspended状态
     */
    private Boolean suspended;

    /**
     * 是否包含开始表单
     */
    private Boolean hasStartForm;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 任务列表
     */
    private List<TaskDTO> tasks;

    /**
     * 活动列表
     */
    private List<String> activities;

    /**
     * 版本标签
     */
    private String versionTag;

    /**
     * 历史变量存储类型
     */
    private String historicVariableStorageType;

    /**
     * 是否可启动
     */
    private Boolean startable;

    /**
     * 是否有开始表单Key
     */
    private Boolean hasStartFormKey;

    /**
     * 部署来源
     */
    private String deploymentSource;

    /**
     * 历史存活时间
     */
    private Integer historyTimeToLive;

    /**
     * 是否可在任务列表中启动
     */
    private Boolean startableInTasklist;
}
