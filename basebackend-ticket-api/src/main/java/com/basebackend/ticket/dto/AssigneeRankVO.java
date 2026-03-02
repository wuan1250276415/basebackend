package com.basebackend.ticket.dto;

/**
 * 处理人排名
 */
public record AssigneeRankVO(
        Long assigneeId,
        String assigneeName,
        long resolvedCount,
        double avgResolutionHours
) {
}
