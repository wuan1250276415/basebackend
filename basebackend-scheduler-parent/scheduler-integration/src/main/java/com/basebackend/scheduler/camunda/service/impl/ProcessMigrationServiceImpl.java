package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.scheduler.camunda.dto.ProcessInstanceMigrationRequest;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.ProcessMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.migration.MigrationPlanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 流程实例迁移服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMigrationServiceImpl implements ProcessMigrationService {

    private final RuntimeService runtimeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrateProcessInstances(ProcessInstanceMigrationRequest request) {
        try {
            log.info("Starting process migration from {} to {} for {} instances",
                    request.getSourceProcessDefinitionId(),
                    request.getTargetProcessDefinitionId(),
                    request.getProcessInstanceIds().size());

            // 1. 创建迁移计划
            MigrationPlanBuilder builder = runtimeService.createMigrationPlan(
                    request.getSourceProcessDefinitionId(),
                    request.getTargetProcessDefinitionId())
                    .mapEqualActivities(); // 自动映射ID相同的活动

            // 手动映射指令
            if (request.getInstructions() != null) {
                for (ProcessInstanceMigrationRequest.MigrationInstructionDTO instr : request.getInstructions()) {
                    if (instr.getSourceActivityId() != null && instr.getTargetActivityId() != null) {
                        builder.mapActivities(instr.getSourceActivityId(), instr.getTargetActivityId());
                        if (instr.isUpdateEventTrigger()) {
                            // complex configuration might be needed here, simplified for now
                        }
                    }
                }
            }

            MigrationPlan migrationPlan = builder.build();

            // 2. 执行迁移
            var executionBuilder = runtimeService.newMigration(migrationPlan)
                    .processInstanceIds(request.getProcessInstanceIds());

            // 根据配置添加选项
            if (request.isSkipCustomListeners()) {
                executionBuilder.skipCustomListeners();
            }
            if (request.isSkipIoMappings()) {
                executionBuilder.skipIoMappings();
            }

            executionBuilder.executeAsync(); // 建议异步执行以避免超时

            log.info("Process migration submitted successfully (Async)");

        } catch (Exception e) {
            log.error("Process migration failed", e);
            throw new CamundaServiceException("流程迁移失败: " + e.getMessage(), e);
        }
    }
}
