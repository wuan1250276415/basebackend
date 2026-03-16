package com.basebackend.ticket.service;

import com.basebackend.ticket.dto.TicketCategoryDTO;
import com.basebackend.ticket.dto.TicketCategoryTreeVO;
import com.basebackend.ticket.entity.TicketCategory;

import java.util.List;

/**
 * 工单分类服务
 */
public interface TicketCategoryService {

    /**
     * 获取分类树
     */
    List<TicketCategoryTreeVO> tree();

    /**
     * 根据ID查询分类
     */
    TicketCategory getById(Long id);

    /**
     * 创建分类
     */
    void create(TicketCategoryDTO dto);

    /**
     * 更新分类
     */
    void update(Long id, TicketCategoryDTO dto);

    /**
     * 删除分类
     */
    void delete(Long id);
}
