package com.basebackend.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.ticket.dto.TicketCommentDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketComment;
import com.basebackend.ticket.enums.CommentType;
import com.basebackend.ticket.mapper.TicketCommentMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketCommentService;
import com.basebackend.ticket.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工单评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCommentServiceImpl implements TicketCommentService {

    private final TicketCommentMapper commentMapper;
    private final TicketMapper ticketMapper;
    private final AuditHelper auditHelper;

    @Override
    public List<TicketComment> listByTicketId(Long ticketId) {
        LambdaQueryWrapper<TicketComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketComment::getTicketId, ticketId)
                .orderByAsc(TicketComment::getCreateTime);
        return commentMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public TicketComment add(Long ticketId, TicketCommentDTO dto) {
        log.info("添加工单评论: ticketId={}", ticketId);

        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setContent(dto.content());
        comment.setType(dto.type() != null ? dto.type() : CommentType.COMMENT.name());
        comment.setIsInternal(dto.isInternal() != null ? dto.isInternal() : 0);
        comment.setParentId(dto.parentId() != null ? dto.parentId() : 0L);
        comment.setCreatorName(UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "");

        // 从工单获取 tenantId
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket != null) {
            comment.setTenantId(ticket.getTenantId());
        }

        auditHelper.setCreateAuditFields(comment);
        commentMapper.insert(comment);

        // 更新工单评论计数
        if (ticket != null) {
            ticket.setCommentCount(ticket.getCommentCount() + 1);
            ticketMapper.updateById(ticket);
        }

        return comment;
    }

    @Override
    @Transactional
    public void delete(Long ticketId, Long commentId) {
        log.info("删除工单评论: ticketId={}, commentId={}", ticketId, commentId);
        commentMapper.deleteById(commentId);

        // 更新工单评论计数
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket != null && ticket.getCommentCount() > 0) {
            ticket.setCommentCount(ticket.getCommentCount() - 1);
            ticketMapper.updateById(ticket);
        }
    }

    @Override
    @Transactional
    public void addSystemComment(Long ticketId, String content) {
        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setContent(content);
        comment.setType(CommentType.SYSTEM.name());
        comment.setIsInternal(0);
        comment.setParentId(0L);
        comment.setCreatorName("系统");

        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket != null) {
            comment.setTenantId(ticket.getTenantId());
        }

        auditHelper.setCreateAuditFields(comment);
        commentMapper.insert(comment);

        if (ticket != null) {
            ticket.setCommentCount(ticket.getCommentCount() + 1);
            ticketMapper.updateById(ticket);
        }
    }
}
