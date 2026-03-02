package com.basebackend.ticket.controller;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.observability.metrics.annotations.Counted;
import com.basebackend.observability.metrics.annotations.Timed;
import com.basebackend.observability.slo.annotation.SloMonitored;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.api.model.scheduler.TaskFeignDTO;
import com.basebackend.ticket.dto.TicketApprovalSubmitDTO;
import com.basebackend.ticket.dto.TicketCcDTO;
import com.basebackend.ticket.entity.TicketApproval;
import com.basebackend.ticket.entity.TicketCc;
import com.basebackend.ticket.enums.ApprovalAction;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.mapper.TicketCcMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketApprovalService;
import com.basebackend.ticket.service.TicketCommentService;
import com.basebackend.ticket.service.TicketService;
import com.basebackend.ticket.service.TicketWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单审批控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/tickets/{ticketId}/approvals")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单审批", description = "工单审批操作及记录查询")
public class TicketApprovalController {

    private final TicketApprovalService approvalService;
    private final TicketService ticketService;
    private final TicketCommentService commentService;
    private final TicketWorkflowService workflowService;
    private final TicketCcMapper ccMapper;
    private final TicketMapper ticketMapper;

    @GetMapping
    @Operation(summary = "审批记录列表", description = "查询工单的审批记录")
    @OperationLog(operation = "查询工单审批记录", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:approve")
    public Result<List<TicketApproval>> list(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        log.info("查询工单审批记录: ticketId={}", ticketId);
        List<TicketApproval> approvals = approvalService.listByTicketId(ticketId);
        return Result.success("查询成功", approvals);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交审批", description = "提交工单进入Camunda审批流程")
    @OperationLog(operation = "提交工单审批", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:approve:submit")
    @Timed(name = "ticket.approval.submit")
    @Counted(name = "ticket.approval.submit.count")
    @SloMonitored(sloName = "ticket.approval.latency")
    public Result<String> submit(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestBody @Valid TicketApprovalSubmitDTO dto) {
        log.info("提交工单审批: ticketId={}, approver1={}, approver2={}",
                ticketId, dto.approver1(), dto.approver2());

        String processInstanceId = workflowService.startApproval(ticketId, dto.approver1(), dto.approver2());
        commentService.addSystemComment(ticketId, "工单已提交审批，流程ID: " + processInstanceId);
        return Result.success("已提交审批", processInstanceId);
    }

    @GetMapping("/tasks")
    @Operation(summary = "活跃审批任务", description = "查询工单的当前活跃审批任务")
    @OperationLog(operation = "查询活跃审批任务", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:approve")
    public Result<List<TaskFeignDTO>> activeTasks(
            @Parameter(description = "工单ID") @PathVariable Long ticketId) {
        log.info("查询活跃审批任务: ticketId={}", ticketId);
        List<TaskFeignDTO> tasks = workflowService.getActiveTasks(ticketId);
        return Result.success("查询成功", tasks);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过", description = "审批通过工单")
    @OperationLog(operation = "审批通过工单", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:approve")
    public Result<String> approve(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String opinion) {
        log.info("审批通过工单: ticketId={}", ticketId);

        Long approverId = UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L;
        String approverName = UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "";

        approvalService.record(ticketId, taskId, taskName,
                approverId, approverName, ApprovalAction.APPROVE, opinion, null, null);

        // 完成 Camunda 审批任务
        if (taskId != null && !taskId.isBlank()) {
            workflowService.completeTask(taskId, ApprovalAction.APPROVE, opinion, String.valueOf(approverId));
        } else {
            ticketService.changeStatus(ticketId, TicketStatus.APPROVED, opinion);
        }

        commentService.addSystemComment(ticketId, approverName + " 审批通过" + (opinion != null ? ": " + opinion : ""));
        return Result.success("审批通过");
    }

    @PostMapping("/reject")
    @Operation(summary = "审批拒绝", description = "审批拒绝工单")
    @OperationLog(operation = "审批拒绝工单", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:approve")
    public Result<String> reject(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String opinion) {
        log.info("审批拒绝工单: ticketId={}", ticketId);

        Long approverId = UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L;
        String approverName = UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "";

        approvalService.record(ticketId, taskId, taskName,
                approverId, approverName, ApprovalAction.REJECT, opinion, null, null);

        // 完成 Camunda 审批任务
        if (taskId != null && !taskId.isBlank()) {
            workflowService.completeTask(taskId, ApprovalAction.REJECT, opinion, String.valueOf(approverId));
        } else {
            ticketService.changeStatus(ticketId, TicketStatus.REJECTED, opinion);
        }

        commentService.addSystemComment(ticketId, approverName + " 审批拒绝" + (opinion != null ? ": " + opinion : ""));
        return Result.success("审批已拒绝");
    }

    @PostMapping("/return")
    @Operation(summary = "退回", description = "退回工单")
    @OperationLog(operation = "退回工单", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:approve")
    public Result<String> returnTicket(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String opinion) {
        log.info("退回工单: ticketId={}", ticketId);

        Long approverId = UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L;
        String approverName = UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "";

        approvalService.record(ticketId, taskId, taskName,
                approverId, approverName, ApprovalAction.RETURN, opinion, null, null);

        // 完成 Camunda 审批任务（退回）
        if (taskId != null && !taskId.isBlank()) {
            workflowService.completeTask(taskId, ApprovalAction.RETURN, opinion, String.valueOf(approverId));
        }

        ticketService.changeStatus(ticketId, TicketStatus.OPEN, opinion);
        commentService.addSystemComment(ticketId, approverName + " 退回工单" + (opinion != null ? ": " + opinion : ""));
        return Result.success("工单已退回");
    }

    @PostMapping("/delegate")
    @Operation(summary = "转办", description = "转办工单审批")
    @OperationLog(operation = "转办工单审批", businessType = BusinessType.UPDATE)
    @RequiresPermission("ticket:approve:delegate")
    public Result<String> delegate(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String taskName,
            @RequestParam Long delegateToId,
            @RequestParam String delegateToName,
            @RequestParam(required = false) String opinion) {
        log.info("转办工单审批: ticketId={}, delegateTo={}", ticketId, delegateToName);

        Long approverId = UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L;
        String approverName = UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "";

        approvalService.record(ticketId, taskId, taskName,
                approverId, approverName, ApprovalAction.DELEGATE, opinion,
                delegateToId, delegateToName);

        // 委派 Camunda 审批任务
        if (taskId != null && !taskId.isBlank()) {
            workflowService.delegateTask(taskId, String.valueOf(approverId), String.valueOf(delegateToId));
        }

        commentService.addSystemComment(ticketId,
                approverName + " 将审批转办给 " + delegateToName + (opinion != null ? ": " + opinion : ""));
        return Result.success("已转办");
    }

    @PostMapping("/cc")
    @Operation(summary = "抄送", description = "抄送工单给其他人")
    @OperationLog(operation = "抄送工单", businessType = BusinessType.INSERT)
    @RequiresPermission("ticket:approve:cc")
    public Result<String> cc(
            @Parameter(description = "工单ID") @PathVariable Long ticketId,
            @RequestBody @Valid TicketCcDTO dto) {
        log.info("抄送工单: ticketId={}, userIds={}", ticketId, dto.userIds());

        var ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return Result.error("工单不存在");
        }

        for (int i = 0; i < dto.userIds().size(); i++) {
            TicketCc cc = new TicketCc();
            cc.setTenantId(ticket.getTenantId());
            cc.setTicketId(ticketId);
            cc.setUserId(dto.userIds().get(i));
            cc.setUserName(dto.userNames() != null && i < dto.userNames().size()
                    ? dto.userNames().get(i) : "");
            cc.setIsRead(0);
            cc.setCreateTime(LocalDateTime.now());
            ccMapper.insert(cc);
        }

        return Result.success("抄送成功");
    }
}
