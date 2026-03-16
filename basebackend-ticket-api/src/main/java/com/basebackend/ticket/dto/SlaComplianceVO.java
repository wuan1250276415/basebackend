package com.basebackend.ticket.dto;

/**
 * SLA 合规率统计
 */
public record SlaComplianceVO(
        long totalCount,
        long breachedCount,
        double complianceRate
) {
}
