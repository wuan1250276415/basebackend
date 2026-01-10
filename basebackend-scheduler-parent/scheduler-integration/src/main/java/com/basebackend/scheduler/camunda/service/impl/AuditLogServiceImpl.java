package com.basebackend.scheduler.camunda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basebackend.scheduler.camunda.entity.AuditLogEntity;
import com.basebackend.scheduler.camunda.mapper.AuditLogMapper;
import com.basebackend.scheduler.camunda.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service("camundaAuditLogService")
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLogEntity> implements AuditLogService {

    private final HistoryService historyService;
    private final TaskService taskService;

    // Use Lazy injection to avoid potential circular dependencies if TaskService
    // depends on this
    public AuditLogServiceImpl(@Lazy HistoryService historyService, @Lazy TaskService taskService) {
        this.historyService = historyService;
        this.taskService = taskService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void log(String type, String taskId, String instanceId, String operatorId, String comment,
            String targetUser) {
        try {
            AuditLogEntity audit = new AuditLogEntity()
                    .setAuditType(type)
                    .setTaskId(taskId)
                    .setProcessInstanceId(instanceId)
                    .setOperatorId(operatorId)
                    .setComment(comment)
                    .setTargetUserId(targetUser);

            // 尝试补充额外信息
            if (taskId != null) {
                // 先查运行中，再查历史
                String taskName = null;
                try {
                    org.camunda.bpm.engine.task.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
                    if (task != null) {
                        taskName = task.getName();
                        if (instanceId == null)
                            instanceId = task.getProcessInstanceId();
                    } else {
                        HistoricTaskInstance hTask = historyService.createHistoricTaskInstanceQuery().taskId(taskId)
                                .singleResult();
                        if (hTask != null) {
                            taskName = hTask.getName();
                            if (instanceId == null)
                                instanceId = hTask.getProcessInstanceId();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch task details for audit log: {}", taskId);
                }
                audit.setTaskName(taskName);
            }

            if (audit.getProcessInstanceId() != null) {
                try {
                    HistoricProcessInstance inst = historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(audit.getProcessInstanceId()).singleResult();
                    if (inst != null) {
                        audit.setBusinessKey(inst.getBusinessKey());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch process instance details for audit log: {}",
                            audit.getProcessInstanceId());
                }
            }

            // Set BaseEntity fields
            audit.setCreateBy(isValidUserId(operatorId) ? Long.parseLong(operatorId) : 0L);
            audit.setUpdateBy(isValidUserId(operatorId) ? Long.parseLong(operatorId) : 0L);

            this.save(audit);
            log.info("Audit log recorded: type={}, task={}, operator={}", type, taskId, operatorId);

        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage(), e);
            // 不抛出异常，避免审计失败影响主业务流程
        }
    }

    private boolean isValidUserId(String userId) {
        if (userId == null)
            return false;
        try {
            Long.parseLong(userId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
