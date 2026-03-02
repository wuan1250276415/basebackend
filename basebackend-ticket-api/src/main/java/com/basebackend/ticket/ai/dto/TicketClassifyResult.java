package com.basebackend.ticket.ai.dto;

/**
 * AI 工单分类结果
 */
public record TicketClassifyResult(
        Long categoryId,
        String categoryName,
        Double confidence,
        String reasoning
) {
}
