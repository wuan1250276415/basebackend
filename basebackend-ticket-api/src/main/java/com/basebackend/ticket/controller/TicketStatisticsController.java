package com.basebackend.ticket.controller;

import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.observability.metrics.annotations.Timed;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.*;
import com.basebackend.ticket.service.TicketStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 工单统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/statistics")
@RequiredArgsConstructor
@Tag(name = "工单统计", description = "工单统计数据查询")
public class TicketStatisticsController {

    private final TicketStatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "统计概览", description = "获取工单统计概览数据")
    @OperationLog(operation = "查询工单统计概览", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.overview")
    public Result<TicketOverviewVO> overview() {
        log.info("查询工单统计概览");
        TicketOverviewVO overview = statisticsService.overview();
        return Result.success("查询成功", overview);
    }

    @GetMapping("/by-category")
    @Operation(summary = "按分类统计", description = "按分类统计工单数量")
    @OperationLog(operation = "按分类统计工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.byCategory")
    public Result<Map<String, Long>> byCategory() {
        log.info("按分类统计工单");
        Map<String, Long> result = statisticsService.countByCategory();
        return Result.success("查询成功", result);
    }

    @GetMapping("/by-status")
    @Operation(summary = "按状态统计", description = "按状态统计工单数量")
    @OperationLog(operation = "按状态统计工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.byStatus")
    public Result<Map<String, Long>> byStatus() {
        log.info("按状态统计工单");
        Map<String, Long> result = statisticsService.countByStatus();
        return Result.success("查询成功", result);
    }

    @GetMapping("/trend")
    @Operation(summary = "工单趋势", description = "获取工单趋势数据（近N天）")
    @OperationLog(operation = "查询工单趋势", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.trend")
    public Result<List<TrendPointVO>> trend(
            @Parameter(description = "天数") @RequestParam(defaultValue = "30") int days) {
        log.info("查询工单趋势: days={}", days);
        List<TrendPointVO> result = statisticsService.getTrend(days);
        return Result.success("查询成功", result);
    }

    @GetMapping("/resolution-time")
    @Operation(summary = "解决时间统计", description = "获取工单解决时间统计（平均/中位/P90）")
    @OperationLog(operation = "查询工单解决时间统计", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.resolutionTime")
    public Result<ResolutionTimeVO> resolutionTime() {
        log.info("查询工单解决时间统计");
        ResolutionTimeVO result = statisticsService.getResolutionTimeStats();
        return Result.success("查询成功", result);
    }

    @GetMapping("/sla-compliance")
    @Operation(summary = "SLA合规率", description = "获取工单SLA合规率")
    @OperationLog(operation = "查询SLA合规率", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.slaCompliance")
    public Result<SlaComplianceVO> slaCompliance() {
        log.info("查询SLA合规率");
        SlaComplianceVO result = statisticsService.getSlaComplianceRate();
        return Result.success("查询成功", result);
    }

    @GetMapping("/top-assignees")
    @Operation(summary = "处理人排名", description = "获取处理人排名（按解决工单数）")
    @OperationLog(operation = "查询处理人排名", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    @Timed(name = "ticket.statistics.topAssignees")
    public Result<List<AssigneeRankVO>> topAssignees(
            @Parameter(description = "排名数量") @RequestParam(defaultValue = "10") int limit) {
        log.info("查询处理人排名: limit={}", limit);
        List<AssigneeRankVO> result = statisticsService.getTopAssignees(limit);
        return Result.success("查询成功", result);
    }
}
