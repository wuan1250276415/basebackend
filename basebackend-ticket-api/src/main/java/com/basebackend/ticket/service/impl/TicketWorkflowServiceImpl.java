package com.basebackend.ticket.service.impl;

import com.basebackend.api.model.scheduler.ProcessDefinitionStartRequest;
import com.basebackend.api.model.scheduler.TaskActionRequest;
import com.basebackend.api.model.scheduler.TaskFeignDTO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.ApprovalAction;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketWorkflowService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.service.client.scheduler.ProcessDefinitionServiceClient;
import com.basebackend.service.client.scheduler.TaskServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单工作流服务实现
 * <p>通过 ProcessDefinitionServiceClient / TaskServiceClient 调用 scheduler-camunda 服务</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketWorkflowServiceImpl implements TicketWorkflowService {

    private static final String PROCESS_KEY = "ticket-approval";

    private final ProcessDefinitionServiceClient processDefinitionClient;
    private final TaskServiceClient taskServiceClient;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public String startApproval(Long ticketId, String approver1, String approver2) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + ticketId);
        }

        log.info("启动审批流程: ticketId={}, ticketNo={}, approver1={}, approver2={}",
                ticketId, ticket.getTicketNo(), approver1, approver2);

        Map<String, Object> variables = new HashMap<>();
        variables.put("ticketId", ticketId);
        variables.put("ticketNo", ticket.getTicketNo());
        variables.put("reporterId", ticket.getReporterId());
        variables.put("approver1", approver1);
        variables.put("approver2", approver2);
        variables.put("needLevel2", approver2 != null);

        String starter = UserContextHolder.getUserId() != null
                ? String.valueOf(UserContextHolder.getUserId()) : null;

        ProcessDefinitionStartRequest request = new ProcessDefinitionStartRequest(
                PROCESS_KEY,
                null,
                "TICKET-" + ticketId,
                null,
                variables,
                starter
        );

        Result<String> result = processDefinitionClient.startProcessInstance(request);
        if (result == null || result.getData() == null) {
            throw new RuntimeException("启动审批流程失败: ticketId=" + ticketId);
        }

        String processInstanceId = result.getData();
        log.info("审批流程已启动: ticketId={}, processInstanceId={}", ticketId, processInstanceId);

        // 更新工单的流程实例ID和状态
        ticketMapper.update(null, new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, ticketId)
                .set(Ticket::getProcessInstanceId, processInstanceId)
                .set(Ticket::getStatus, TicketStatus.PENDING_APPROVAL.name()));

        return processInstanceId;
    }

    @Override
    public void completeTask(String taskId, ApprovalAction action, String opinion, String userId) {
        log.info("完成审批任务: taskId={}, action={}, userId={}", taskId, action, userId);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", action == ApprovalAction.APPROVE);
        variables.put("action", action.name());

        TaskActionRequest request = new TaskActionRequest(
                taskId,
                userId,
                variables,
                null,
                opinion
        );

        taskServiceClient.complete(taskId, request);
        log.info("审批任务已完成: taskId={}", taskId);
    }

    @Override
    public void delegateTask(String taskId, String userId, String delegateUserId) {
        log.info("委派审批任务: taskId={}, from={}, to={}", taskId, userId, delegateUserId);
        taskServiceClient.delegate(taskId, userId, delegateUserId);
    }

    @Override
    public List<TaskFeignDTO> getActiveTasks(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || ticket.getProcessInstanceId() == null) {
            return Collections.emptyList();
        }

        Result<List<TaskFeignDTO>> result = taskServiceClient
                .getActiveTasksByProcessInstance(ticket.getProcessInstanceId());
        if (result == null || result.getData() == null) {
            return Collections.emptyList();
        }
        return result.getData();
    }
}
