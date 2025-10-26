package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 订单审批任务委托
 */
@Slf4j
@Component("orderApprovalDelegate")
public class OrderApprovalDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String orderId = (String) execution.getVariable("orderId");
        Double amount = (Double) execution.getVariable("amount");
        Boolean approved = (Boolean) execution.getVariable("approved");

        log.info("处理订单审批结果: orderId={}, amount={}, approved={}",
                orderId, amount, approved);

        if (Boolean.TRUE.equals(approved)) {
            // 审批通过，执行订单处理
            log.info("订单审批通过，开始处理订单: {}", orderId);

            // TODO: 调用订单服务处理订单
            // orderService.processOrder(orderId);

            execution.setVariable("orderStatus", "APPROVED");
            execution.setVariable("processTime", System.currentTimeMillis());
        } else {
            // 审批拒绝
            log.info("订单审批拒绝: {}", orderId);
            execution.setVariable("orderStatus", "REJECTED");
        }
    }
}
