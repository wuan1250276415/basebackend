package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.TicketAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单附件 Mapper
 */
@Mapper
public interface TicketAttachmentMapper extends BaseMapper<TicketAttachment> {
}
