package com.basebackend.scheduler.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 工作流实例持久化实体
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("workflow_instance")
public class WorkflowInstanceEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 工作流定义ID
     */
    @TableField("definition_id")
    private String definitionId;

    /**
     * 实例状态 (PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED/PAUSED)
     */
    @TableField("status")
    private String status;

    /**
     * 活跃节点集合 (JSON格式)
     */
    @TableField("active_nodes")
    private String activeNodesJson;

    /**
     * 上下文参数 (JSON格式)
     */
    @TableField("context")
    private String contextJson;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private Instant startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private Instant endTime;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Instant createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;

    /**
     * 版本号 (用于乐观锁)
     */
    @Version
    @TableField("version")
    private Long version;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
