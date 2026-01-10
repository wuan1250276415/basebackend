package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务抄送实体
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_task_cc")
public class TaskCCEntity extends BaseEntity {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 抄送给用户ID
     */
    private String userId;

    /**
     * 抄送发起人ID
     */
    private String initiatorId;

    /**
     * 状态：UNREAD, READ
     */
    private String status;
}
