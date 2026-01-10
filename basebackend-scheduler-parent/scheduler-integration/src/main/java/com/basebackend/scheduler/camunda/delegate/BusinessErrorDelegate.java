package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 业务错误触发委托
 *
 * <p>
 * 用于在流程中主动抛出 BPMN 错误，用于测试错误边界事件或实现特定的业务错误逻辑。
 * </p>
 *
 * <p>
 * 输入变量：
 * <ul>
 * <li>errorCode (String, required): 错误代码</li>
 * <li>errorMessage (String, optional): 错误消息</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@Component("businessErrorDelegate")
public class BusinessErrorDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String errorCode = (String) execution.getVariable("errorCode");
        String errorMessage = (String) execution.getVariable("errorMessage");

        if (errorCode == null) {
            log.warn("BusinessErrorDelegate called without 'errorCode' variable. Defaulting to 'GenericError'.");
            errorCode = "GenericError";
        }
        if (errorMessage == null) {
            errorMessage = "Business error triggered by delegate.";
        }

        log.info("Throwing BpmnError: code={}, message={}, processInstanceId={}",
                errorCode, errorMessage, execution.getProcessInstanceId());

        throw new BpmnError(errorCode, errorMessage);
    }
}
