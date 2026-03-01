package com.basebackend.ticket.service;

import com.basebackend.ticket.dto.TicketOverviewVO;

import java.util.Map;

/**
 * 工单统计服务
 */
public interface TicketStatisticsService {

    /**
     * 统计概览
     */
    TicketOverviewVO overview();

    /**
     * 按分类统计工单数量
     *
     * @return key=分类名称, value=数量
     */
    Map<String, Long> countByCategory();

    /**
     * 按状态统计工单数量
     *
     * @return key=状态, value=数量
     */
    Map<String, Long> countByStatus();
}
