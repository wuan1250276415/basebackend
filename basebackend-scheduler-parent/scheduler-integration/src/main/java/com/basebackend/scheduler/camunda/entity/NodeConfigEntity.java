package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点配置实体
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_node_config")
public class NodeConfigEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 节点Key(ActivityId)
     */
    private String nodeKey;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 挂载表单Key
     */
    private String formKey;

    /**
     * 候选人规则JSON
     */
    private String candidateRule;

    /**
     * 超时策略JSON
     */
    private String timeoutStrategy;

    /**
     * 按钮配置JSON
     */
    private String buttonsConfig;

    /**
     * 表单字段权限JSON
     * <p>
     * 格式：{ "fieldName": "READONLY/EDITABLE/HIDDEN" }
     * </p>
     */
    private String fieldPermissions;
}
