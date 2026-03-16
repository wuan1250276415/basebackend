package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.TicketStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单状态变更日志 Mapper
 */
@Mapper
public interface TicketStatusLogMapper extends BaseMapper<TicketStatusLog> {
}
