package com.basebackend.scheduler.camunda.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Saga 补偿委托
 *
 * <p>
 * 用于实现 Saga 模式中的补偿逻辑（Compensation Handler）。
 * 通常绑定到 BPMN 的补偿边界事件（Compensation Boundary Event）触发的任务上。
 * </p>
 *
 * <p>
 * 本委托主要负责记录补偿日志，并可扩展为调用外部服务的反向操作（Undo）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Component("sagaCompensationDelegate")
@RequiredArgsConstructor
public class SagaCompensationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String currentActivityId = execution.getCurrentActivityId();

        // 假设补偿相关的上下文信息存储在特定的变量中
        // 例如：originalTransactionId, compensationReason
        String originalTxId = (String) execution.getVariable("originalTransactionId");

        log.info("Executing Saga Compensation: processInstanceId={}, activityId={}, originalTxId={}",
                processInstanceId, currentActivityId, originalTxId);

        // TODO: 在此处实现具体的补偿逻辑
        // 例如：调用 MicroserviceCallDelegate 的逻辑来调用 "Undo API"
        // 目前仅作为占位符和日志记录

        execution.setVariable("compensationExecuted", true);
        execution.setVariable("compensationTime", java.time.LocalDateTime.now().toString());

        log.info("Saga Compensation completed successfully for processInstanceId={}", processInstanceId);
    }
}
