package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流模板实体
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_process_template")
public class ProcessTemplateEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板标识(ProcessKey)
     */
    private String templateKey;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 图标
     */
    private String icon;

    /**
     * 分类
     */
    private String category;

    /**
     * 版本标签
     */
    private String versionTag;

    /**
     * Camunda部署ID
     */
    private String deploymentId;

    /**
     * BPMN资源名称
     */
    private String resourceName;

    /**
     * 描述
     */
    private String description;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 状态(0:草稿 1:发布 2:停用)
     */
    private Integer status;

    /**
     * 租户ID
     */
    private String tenantId;
}
