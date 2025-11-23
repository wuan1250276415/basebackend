package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程定义 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
@Data
public class ProcessDefinitionFeignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    private String id;

    /**
     * 流程定义键
     */
    private String key;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 流程定义类别
     */
    private String category;

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 是否Suspended（挂起）
     */
    private boolean suspended;

    /**
     * 流程定义描述
     */
    private String description;

    /**
     * BPMN 2.0 XML文件的唯一标识符
     */
    private String resourceName;

    /**
     * 流程图文件的名称
     */
    private String diagramResourceName;

    /**
     * DGRM规范的URI
     */
    private String startFormKey;

    /**
     * 版本标签
     */
    private String versionTag;

    /**
     * 候选开始活动ID集合（多个ID用逗号分隔）
     */
    private String startableInTasklist;

    /**
     * 是否可由API启动
     */
    private boolean startable;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 最后修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModified;

    /**
     * 最后修改人
     */
    private String lastModifiedBy;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 候选开始活动信息
     */
    private String startActivityId;

    /**
     * 部署时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deploymentTime;
}
