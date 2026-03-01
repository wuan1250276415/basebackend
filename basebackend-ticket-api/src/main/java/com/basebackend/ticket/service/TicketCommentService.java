package com.basebackend.ticket.service;

import com.basebackend.ticket.dto.TicketCommentDTO;
import com.basebackend.ticket.entity.TicketComment;

import java.util.List;

/**
 * 工单评论服务
 */
public interface TicketCommentService {

    /**
     * 查询工单的评论列表
     */
    List<TicketComment> listByTicketId(Long ticketId);

    /**
     * 添加评论
     */
    TicketComment add(Long ticketId, TicketCommentDTO dto);

    /**
     * 删除评论（逻辑删除）
     */
    void delete(Long ticketId, Long commentId);

    /**
     * 添加系统消息
     */
    void addSystemComment(Long ticketId, String content);
}
