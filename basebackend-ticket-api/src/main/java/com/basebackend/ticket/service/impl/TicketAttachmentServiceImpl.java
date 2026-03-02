package com.basebackend.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketAttachment;
import com.basebackend.ticket.mapper.TicketAttachmentMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketAttachmentService;
import com.basebackend.ticket.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工单附件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAttachmentServiceImpl implements TicketAttachmentService {

    private final TicketAttachmentMapper attachmentMapper;
    private final TicketMapper ticketMapper;
    private final AuditHelper auditHelper;

    @Override
    public List<TicketAttachment> listByTicketId(Long ticketId) {
        LambdaQueryWrapper<TicketAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketAttachment::getTicketId, ticketId)
                .orderByAsc(TicketAttachment::getCreateTime);
        return attachmentMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void add(Long ticketId, Long fileId, String fileName, Long fileSize,
                    String fileType, String fileUrl) {
        log.info("关联附件到工单: ticketId={}, fileId={}", ticketId, fileId);

        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + ticketId);
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTenantId(ticket.getTenantId());
        attachment.setTicketId(ticketId);
        attachment.setFileId(fileId);
        attachment.setFileName(fileName);
        attachment.setFileSize(fileSize);
        attachment.setFileType(fileType);
        attachment.setFileUrl(fileUrl);
        attachment.setUploadBy(UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L);

        auditHelper.setCreateAuditFields(attachment);
        attachmentMapper.insert(attachment);

        // 更新工单附件计数
        ticket.setAttachmentCount(ticket.getAttachmentCount() + 1);
        ticketMapper.updateById(ticket);
    }

    @Override
    @Transactional
    public void addBatch(Long ticketId, List<Long> fileIds) {
        // 批量关联的简化实现，实际场景应调用 FileServiceClient 获取文件信息
        for (Long fileId : fileIds) {
            add(ticketId, fileId, "", 0L, "", "");
        }
    }

    @Override
    @Transactional
    public void delete(Long ticketId, Long attachmentId) {
        log.info("移除工单附件: ticketId={}, attachmentId={}", ticketId, attachmentId);
        TicketAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || !ticketId.equals(attachment.getTicketId())) {
            throw new RuntimeException("附件不存在或不属于该工单: " + attachmentId);
        }
        attachmentMapper.deleteById(attachmentId);

        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket != null) {
            long latestCount = attachmentMapper.selectCount(new LambdaQueryWrapper<TicketAttachment>()
                    .eq(TicketAttachment::getTicketId, ticketId));
            ticket.setAttachmentCount((int) latestCount);
            ticketMapper.updateById(ticket);
        }
    }
}
