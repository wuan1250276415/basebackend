package com.basebackend.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.event.DomainEventPublisher;
import com.basebackend.ticket.entity.TicketApproval;
import com.basebackend.ticket.enums.ApprovalAction;
import com.basebackend.ticket.event.TicketApprovedEvent;
import com.basebackend.ticket.mapper.TicketApprovalMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单审批服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketApprovalServiceImpl implements TicketApprovalService {

    private final TicketApprovalMapper approvalMapper;
    private final TicketMapper ticketMapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public List<TicketApproval> listByTicketId(Long ticketId) {
        LambdaQueryWrapper<TicketApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketApproval::getTicketId, ticketId)
                .orderByDesc(TicketApproval::getCreateTime);
        return approvalMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public TicketApproval record(Long ticketId, String taskId, String taskName,
                                 Long approverId, String approverName,
                                 ApprovalAction action, String opinion,
                                 Long delegateToId, String delegateToName) {
        log.info("记录审批操作: ticketId={}, action={}, approver={}", ticketId, action, approverName);

        var ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + ticketId);
        }

        TicketApproval approval = new TicketApproval();
        approval.setTenantId(ticket.getTenantId());
        approval.setTicketId(ticketId);
        approval.setTaskId(taskId);
        approval.setTaskName(taskName);
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setAction(action.name());
        approval.setOpinion(opinion != null ? opinion : "");
        approval.setDelegateToId(delegateToId);
        approval.setDelegateToName(delegateToName);
        approval.setCreateTime(LocalDateTime.now());

        approvalMapper.insert(approval);

        // 发布审批事件
        eventPublisher.publish(new TicketApprovedEvent(
                "ticket-service", ticketId, ticket.getTicketNo(),
                action.name(), approverId, approverName, opinion));

        return approval;
    }
}
