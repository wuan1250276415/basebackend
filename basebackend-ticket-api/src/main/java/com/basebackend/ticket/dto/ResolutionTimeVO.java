package com.basebackend.ticket.dto;

/**
 * 工单解决时间统计
 */
public record ResolutionTimeVO(
        double avgHours,
        double medianHours,
        double p90Hours
) {
}
