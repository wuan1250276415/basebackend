package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.TicketApproval;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单审批记录 Mapper
 */
@Mapper
public interface TicketApprovalMapper extends BaseMapper<TicketApproval> {
}
