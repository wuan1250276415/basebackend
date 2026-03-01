package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单评论 Mapper
 */
@Mapper
public interface TicketCommentMapper extends BaseMapper<TicketComment> {
}
