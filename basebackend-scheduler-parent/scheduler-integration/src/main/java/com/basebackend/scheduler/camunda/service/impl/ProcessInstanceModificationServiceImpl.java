package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceModificationRequest;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.AuditLogService;
import com.basebackend.scheduler.camunda.service.ProcessInstanceModificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstanceModificationBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 流程实例修改服务实现
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInstanceModificationServiceImpl implements ProcessInstanceModificationService {

    private final RuntimeService runtimeService;
    @Qualifier("camundaAuditLogService")
    private final AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyProcessInstance(String instanceId, ProcessInstanceModificationRequest request) {
        try {
            String userId = String.valueOf(UserContextHolder.getUserId());
            log.info("Modifying process instance {}, requested by {}", instanceId, userId);

            ProcessInstanceModificationBuilder builder = runtimeService.createProcessInstanceModification(instanceId);

            // 1. 取消活动
            if (request.getCancelActivityIds() != null) {
                for (String activityId : request.getCancelActivityIds()) {
                    builder.cancelAllForActivity(activityId);
                }
            }

            // 2. 启动活动（跳转）
            if (request.getStartBeforeActivityIds() != null) {
                for (String activityId : request.getStartBeforeActivityIds()) {
                    builder.startBeforeActivity(activityId);
                }
            }
            if (request.getStartAfterActivityIds() != null) {
                for (String activityId : request.getStartAfterActivityIds()) {
                    builder.startAfterActivity(activityId);
                }
            }

            // 3. 设置变量
//            if (request.getVariables() != null) {
//                for (Map.Entry<String, Object> entry : request.getVariables().entrySet()) {
//                    builder(entry.getKey(), entry.getValue());
//                }
//            }

            // 4. 配置监听器
            if (request.isSkipCustomListeners()) {
                // builder interface might vary slightly across versions,
                // but standard API supports creating modification without listeners implicitly
                // if not triggered
                // NOTE: standard modification API executes listeners by default.
                // There isn't a direct "skipCustomListeners" on the builder in all versions,
                // but usually cancellation/start triggers them.
                // We'll proceed with default behavior or check specific API if needed.
                // For this implementation, we assume standard behavior.
            }

            // 5. 执行修改
            builder.execute(request.isSkipCustomListeners(), false); // skipCustomListeners, skipIoMappings

            // 6. 审计日志
            auditLogService.log("INSTANCE_MODIFY", null, instanceId, userId,
                    "流程实例跳转/修改: " + request.getAnnotation(), null);

            log.info("Process instance {} modified successfully", instanceId);

        } catch (Exception e) {
            log.error("Failed to modify process instance {}", instanceId, e);
            throw new CamundaServiceException("流程实例修改失败: " + e.getMessage(), e);
        }
    }
}
