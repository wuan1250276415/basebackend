package com.basebackend.ticket.service;

import com.basebackend.ticket.entity.TicketApproval;
import com.basebackend.ticket.enums.ApprovalAction;

import java.util.List;

/**
 * 工单审批服务
 */
public interface TicketApprovalService {

    /**
     * 查询工单的审批记录
     */
    List<TicketApproval> listByTicketId(Long ticketId);

    /**
     * 记录审批操作
     */
    TicketApproval record(Long ticketId, String taskId, String taskName,
                          Long approverId, String approverName,
                          ApprovalAction action, String opinion,
                          Long delegateToId, String delegateToName);
}
