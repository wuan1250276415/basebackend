package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.TicketCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单分类 Mapper
 */
@Mapper
public interface TicketCategoryMapper extends BaseMapper<TicketCategory> {
}
