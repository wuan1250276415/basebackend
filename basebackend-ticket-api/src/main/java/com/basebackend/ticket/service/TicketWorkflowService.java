package com.basebackend.ticket.service;

import com.basebackend.api.model.scheduler.TaskFeignDTO;
import com.basebackend.ticket.enums.ApprovalAction;

import java.util.List;

/**
 * 工单工作流服务
 * <p>封装 Camunda 流程引擎交互，通过 HttpExchange 服务客户端调用 scheduler-camunda</p>
 */
public interface TicketWorkflowService {

    /**
     * 启动审批流程
     *
     * @param ticketId  工单ID
     * @param approver1 一级审批人ID
     * @param approver2 二级审批人ID（可选，null 表示无需二级审批）
     * @return 流程实例ID
     */
    String startApproval(Long ticketId, String approver1, String approver2);

    /**
     * 完成审批任务
     *
     * @param taskId  Camunda 任务ID
     * @param action  审批动作
     * @param opinion 审批意见
     * @param userId  操作人ID
     */
    void completeTask(String taskId, ApprovalAction action, String opinion, String userId);

    /**
     * 委派任务
     *
     * @param taskId         Camunda 任务ID
     * @param userId         当前操作人ID
     * @param delegateUserId 被委派人ID
     */
    void delegateTask(String taskId, String userId, String delegateUserId);

    /**
     * 获取工单的活跃审批任务
     *
     * @param ticketId 工单ID
     * @return 活跃任务列表
     */
    List<TaskFeignDTO> getActiveTasks(Long ticketId);
}
