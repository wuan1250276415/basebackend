package com.basebackend.ticket.ai;

import com.basebackend.ticket.ai.dto.TicketClassifyResult;

import java.util.List;

/**
 * 工单 AI 智能服务
 */
public interface TicketAiService {

    /**
     * AI 自动分类工单
     *
     * @param title 工单标题
     * @param description 工单描述
     * @return 分类结果（含置信度和推理过程）
     */
    TicketClassifyResult classifyTicket(String title, String description);

    /**
     * AI 总结工单内容
     *
     * @param ticketId 工单ID
     * @return 工单摘要
     */
    String summarizeTicket(Long ticketId);

    /**
     * AI 推荐回复
     *
     * @param ticketId 工单ID
     * @return 推荐回复列表（通常3条）
     */
    List<String> suggestReply(Long ticketId);
}
