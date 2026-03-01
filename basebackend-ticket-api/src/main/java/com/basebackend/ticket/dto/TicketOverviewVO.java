package com.basebackend.ticket.dto;

/**
 * 工单统计概览
 */
public record TicketOverviewVO(
        long totalCount,
        long openCount,
        long inProgressCount,
        long pendingApprovalCount,
        long resolvedCount,
        long closedCount,
        long slaBreachedCount
) {
}
