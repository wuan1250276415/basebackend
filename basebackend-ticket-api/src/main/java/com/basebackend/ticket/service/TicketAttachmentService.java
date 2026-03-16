package com.basebackend.ticket.service;

import com.basebackend.ticket.entity.TicketAttachment;

import java.util.List;

/**
 * 工单附件服务
 */
public interface TicketAttachmentService {

    /**
     * 查询工单的附件列表
     */
    List<TicketAttachment> listByTicketId(Long ticketId);

    /**
     * 关联附件到工单
     */
    void add(Long ticketId, Long fileId, String fileName, Long fileSize, String fileType, String fileUrl);

    /**
     * 批量关联附件
     */
    void addBatch(Long ticketId, List<Long> fileIds);

    /**
     * 移除附件（逻辑删除）
     */
    void delete(Long ticketId, Long attachmentId);
}
