package com.basebackend.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.ticket.dto.TicketListVO;
import com.basebackend.ticket.dto.TicketQueryDTO;

/**
 * 工单 Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 分页查询工单列表（含分类名称）
     */
    IPage<TicketListVO> selectTicketPage(Page<TicketListVO> page, @Param("query") TicketQueryDTO query);
}
