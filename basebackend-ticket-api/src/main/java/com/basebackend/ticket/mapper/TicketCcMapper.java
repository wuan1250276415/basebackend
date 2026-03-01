package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.TicketCc;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单抄送 Mapper
 */
@Mapper
public interface TicketCcMapper extends BaseMapper<TicketCc> {
}
