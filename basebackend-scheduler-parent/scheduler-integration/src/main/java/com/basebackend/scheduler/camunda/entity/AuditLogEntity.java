package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 审计日志实体
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scheduler_audit_log")
public class AuditLogEntity extends BaseEntity {


    /**
     * 审计类型(TASK_COMPLETE, TASK_DELEGATE, etc.)
     */
    private String auditType;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 目标用户ID(如被委托人)
     */
    private String targetUserId;

    /**
     * 操作备注/意见
     */
    private String comment;

    /**
     * 详细信息JSON
     */
    private String details;
}
