package com.basebackend.ticket.service;

import com.basebackend.ticket.dto.*;

import java.util.List;
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

    /**
     * 获取工单趋势数据
     *
     * @param days 天数
     * @return 按日期的趋势数据点
     */
    List<TrendPointVO> getTrend(int days);

    /**
     * 获取工单解决时间统计
     */
    ResolutionTimeVO getResolutionTimeStats();

    /**
     * 获取 SLA 合规率
     */
    SlaComplianceVO getSlaComplianceRate();

    /**
     * 获取处理人排名（按解决工单数）
     *
     * @param limit 前N名
     */
    List<AssigneeRankVO> getTopAssignees(int limit);
}
