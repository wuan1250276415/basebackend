package com.basebackend.feign.dto.scheduler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程定义 Feign DTO
 *
 * @author Claude Code
 * @since 2025-11-25
 */
public record ProcessDefinitionFeignDTO(
        /** 流程定义ID */
        String id,

        /** 流程定义键 */
        String key,

        /** 流程名称 */
        String name,

        /** 版本号 */
        Integer version,

        /** 流程定义类别 */
        String category,

        /** 部署ID */
        String deploymentId,

        /** 是否Suspended（挂起） */
        boolean suspended,

        /** 流程定义描述 */
        String description,

        /** BPMN 2.0 XML文件的唯一标识符 */
        String resourceName,

        /** 流程图文件的名称 */
        String diagramResourceName,

        /** DGRM规范的URI */
        String startFormKey,

        /** 版本标签 */
        String versionTag,

        /** 候选开始活动ID集合（多个ID用逗号分隔） */
        String startableInTasklist,

        /** 是否可由API启动 */
        boolean startable,

        /** 创建时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime created,

        /** 创建人 */
        String createdBy,

        /** 最后修改时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastModified,

        /** 最后修改人 */
        String lastModifiedBy,

        /** 租户ID */
        String tenantId,

        /** 候选开始活动信息 */
        String startActivityId,

        /** 部署时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime deploymentTime
) implements Serializable {
}
