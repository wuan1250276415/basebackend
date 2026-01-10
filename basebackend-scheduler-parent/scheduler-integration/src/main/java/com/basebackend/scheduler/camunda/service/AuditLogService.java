package com.basebackend.scheduler.camunda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.basebackend.scheduler.camunda.entity.AuditLogEntity;

/**
 * 审计日志服务接口
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
public interface AuditLogService extends IService<AuditLogEntity> {

    /**
     * 记录审计日志
     *
     * @param type       审计类型
     * @param taskId     任务ID
     * @param instanceId 流程实例ID
     * @param operatorId 操作人ID
     * @param comment    备注
     * @param targetUser 目标用户（可选）
     */
    void log(String type, String taskId, String instanceId, String operatorId, String comment, String targetUser);
}
