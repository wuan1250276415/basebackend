package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    private String id;

    /**
     * 流程定义Key
     */
    private String key;

    /**
     * 流程定义名称
     */
    private String name;

    /**
     * 流程定义版本
     */
    private Integer version;

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 图表资源名称
     */
    private String diagramResourceName;

    /**
     * 是否挂起
     */
    private Boolean suspended;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 版本标签
     */
    private String versionTag;

    /**
     * 描述
     */
    private String description;

    /**
     * 部署时间
     */
    private Date deploymentTime;
}
