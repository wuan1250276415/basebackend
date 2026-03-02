package com.basebackend.ticket.dto;

import java.time.LocalDate;

/**
 * 工单趋势数据点
 */
public record TrendPointVO(
        LocalDate date,
        long openCount,
        long resolvedCount,
        long closedCount
) {
}
