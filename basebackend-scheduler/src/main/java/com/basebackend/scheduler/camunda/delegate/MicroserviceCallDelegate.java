package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 微服务调用任务委托
 */
@Slf4j
@Component("microserviceCallDelegate")
public class MicroserviceCallDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String serviceName = (String) execution.getVariable("serviceName");
        String operation = (String) execution.getVariable("operation");
        Object payload = execution.getVariable("payload");

        log.info("调用微服务: service={}, operation={}", serviceName, operation);

        try {
            // TODO: 实现实际的微服务调用
            // Object result = restTemplate.postForObject(
            //     serviceUrl + "/" + operation,
            //     payload,
            //     Object.class
            // );

            // 模拟微服务调用
            Thread.sleep(1500);

            // 设置调用结果
            execution.setVariable("callSuccess", true);
            execution.setVariable("callResult", "Success");
            execution.setVariable("callTime", System.currentTimeMillis());

            log.info("微服务调用成功: service={}", serviceName);
        } catch (Exception e) {
            log.error("微服务调用失败: service={}", serviceName, e);
            execution.setVariable("callSuccess", false);
            execution.setVariable("callError", e.getMessage());
            throw e;
        }
    }
}
