package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单审批业务任务委托
 *
 * <p>功能说明：
 * <ul>
 *   <li>处理订单审批业务逻辑</li>
 *   <li>更新订单状态</li>
 *   <li>记录审批历史</li>
 *   <li>通知相关人员（模拟实现）</li>
 * </ul>
 *
 * <p>输入变量：
 * <ul>
 *   <li>orderId (Long, required): 订单ID</li>
 *   <li>approved (Boolean, required): 是否批准</li>
 *   <li>approver (String, required): 审批人</li>
 *   <li>approvalComment (String, optional): 审批意见</li>
 * </ul>
 *
 * <p>输出变量：
 * <ul>
 *   <li>orderStatus (String): 订单状态</li>
 *   <li>orderApproved (Boolean): 审批结果</li>
 *   <li>approvalTime (String): 审批时间</li>
 *   <li>approvalHistory (Map): 审批历史记录</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component("orderApprovalDelegate")
public class OrderApprovalDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();

        log.info("OrderApprovalDelegate started, processInstanceId={}, activityId={}",
                processInstanceId, activityId);

        try {
            // 获取并验证必需的流程变量
            Long orderId = getLongVariable(execution, "orderId");
            Boolean approved = (Boolean) execution.getVariable("approved");
            String approver = (String) execution.getVariable("approver");
            String approvalComment = (String) execution.getVariable("approvalComment");

            validateRequiredVariables(orderId, approved, approver);

            log.info("Processing order approval, orderId={}, approved={}, approver={}",
                    orderId, approved, approver);

            // 处理审批逻辑
            String orderStatus = processApproval(orderId, approved, approver, approvalComment);

            // 构建审批历史记录
            Map<String, Object> approvalHistory = buildApprovalHistory(
                    orderId, approved, approver, approvalComment, orderStatus
            );

            // 记录审批结果到流程变量
            String approvalTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            execution.setVariable("orderStatus", orderStatus);
            execution.setVariable("orderApproved", approved);
            execution.setVariable("approvalTime", approvalTime);
            execution.setVariable("approvalHistory", approvalHistory);

            // 模拟通知相关人员
            notifyStakeholders(orderId, approved, approver, orderStatus);

            log.info("Order approval completed, orderId={}, status={}, approved={}",
                    orderId, orderStatus, approved);

        } catch (IllegalArgumentException ex) {
            // 参数验证失败
            log.error("Order approval validation failed, processInstanceId={}, error={}",
                    processInstanceId, ex.getMessage());

            execution.setVariable("orderApproved", false);
            execution.setVariable("orderStatus", "VALIDATION_FAILED");

            throw new BpmnError("APPROVAL_VALIDATION_ERROR", ex.getMessage());

        } catch (Exception ex) {
            // 审批处理失败
            log.error("Order approval failed, processInstanceId={}", processInstanceId, ex);

            execution.setVariable("orderApproved", false);
            execution.setVariable("orderStatus", "APPROVAL_FAILED");

            throw new BpmnError("APPROVAL_ERROR", "订单审批失败: " + ex.getMessage());
        }
    }

    /**
     * 获取Long类型的流程变量
     */
    private Long getLongVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(variableName + " 不是有效的Long值: " + value);
            }
        }
        throw new IllegalArgumentException(variableName + " 类型不正确: " + value.getClass());
    }

    /**
     * 验证必需的流程变量
     */
    private void validateRequiredVariables(Long orderId, Boolean approved, String approver) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空（orderId）");
        }
        if (approved == null) {
            throw new IllegalArgumentException("审批结果不能为空（approved）");
        }
        if (!StringUtils.hasText(approver)) {
            throw new IllegalArgumentException("审批人不能为空（approver）");
        }
    }

    /**
     * 处理审批逻辑
     *
     * 实际项目中应该调用订单服务更新数据库
     */
    private String processApproval(Long orderId, Boolean approved, String approver, String comment) {
        log.info("Processing approval for order {}, approved={}, approver={}, comment={}",
                orderId, approved, approver, comment);

        // 模拟审批处理逻辑
        if (approved) {
            log.info("Order {} approved by {}", orderId, approver);
            return "APPROVED";
        } else {
            log.info("Order {} rejected by {}, reason: {}", orderId, approver, comment);
            return "REJECTED";
        }
    }

    /**
     * 构建审批历史记录
     */
    private Map<String, Object> buildApprovalHistory(
            Long orderId,
            Boolean approved,
            String approver,
            String comment,
            String status) {

        Map<String, Object> history = new HashMap<>();
        history.put("orderId", orderId);
        history.put("approved", approved);
        history.put("approver", approver);
        history.put("comment", comment != null ? comment : "");
        history.put("status", status);
        history.put("timestamp", LocalDateTime.now().toString());

        return history;
    }

    /**
     * 通知相关人员
     *
     * 实际项目中应该调用通知服务发送邮件/短信/站内信
     */
    private void notifyStakeholders(Long orderId, Boolean approved, String approver, String status) {
        log.info("Notifying stakeholders about order {} approval result: approved={}, status={}, approver={}",
                orderId, approved, status, approver);

        // 模拟通知逻辑
        String message = String.format(
                "订单 %d 已被 %s %s",
                orderId,
                approver,
                approved ? "批准" : "拒绝"
        );

        log.info("Notification message: {}", message);
    }
}
