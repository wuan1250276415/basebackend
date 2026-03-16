package com.basebackend.ticket.search;

import com.basebackend.search.model.SearchResult;
import com.basebackend.ticket.dto.TicketQueryDTO;
import com.basebackend.ticket.entity.Ticket;

/**
 * 工单全文搜索服务
 */
public interface TicketSearchService {

    /**
     * 索引工单文档
     */
    void indexTicket(Ticket ticket);

    /**
     * 删除工单索引
     */
    void removeTicket(Long ticketId);

    /**
     * 全文搜索工单
     *
     * @param keyword 关键词
     * @param filters 过滤条件
     * @param page    页码（从1开始）
     * @param size    每页大小
     * @return 搜索结果（含高亮）
     */
    SearchResult<TicketSearchDocument> search(String keyword, TicketQueryDTO filters, int page, int size);

    /**
     * 全量重建索引
     */
    void reindexAll();
}
