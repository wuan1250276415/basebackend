package com.basebackend.ticket.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.ticket.dto.TicketCreateDTO;
import com.basebackend.ticket.dto.TicketDetailVO;
import com.basebackend.ticket.dto.TicketListVO;
import com.basebackend.ticket.dto.TicketQueryDTO;
import com.basebackend.ticket.dto.TicketUpdateDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.TicketStatus;

/**
 * 工单服务
 */
public interface TicketService {

    /**
     * 创建工单
     */
    Ticket create(TicketCreateDTO dto);

    /**
     * 根据ID查询工单
     */
    Ticket getById(Long id);

    /**
     * 根据工单号查询
     */
    Ticket getByTicketNo(String ticketNo);

    /**
     * 查询工单详情（含评论、附件、状态日志、审批记录、抄送）
     */
    TicketDetailVO getDetail(Long id);

    /**
     * 分页查询工单列表
     */
    IPage<TicketListVO> page(TicketQueryDTO query, Page<Ticket> page);

    /**
     * 更新工单
     */
    void update(Long id, TicketUpdateDTO dto);

    /**
     * 变更工单状态
     */
    void changeStatus(Long id, TicketStatus toStatus, String remark);

    /**
     * 分配处理人
     */
    void assign(Long id, Long assigneeId, String assigneeName);

    /**
     * 关闭工单
     */
    void close(Long id, String remark);

    /**
     * 删除工单（逻辑删除）
     */
    void delete(Long id);

    /**
     * 生成工单编号，格式: TK-yyyyMMdd-NNNN
     */
    String generateTicketNo();
}
