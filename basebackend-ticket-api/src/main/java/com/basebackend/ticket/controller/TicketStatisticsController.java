package com.basebackend.ticket.controller;

import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.TicketOverviewVO;
import com.basebackend.ticket.service.TicketStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<TicketOverviewVO> overview() {
        log.info("查询工单统计概览");
        TicketOverviewVO overview = statisticsService.overview();
        return Result.success("查询成功", overview);
    }

    @GetMapping("/by-category")
    @Operation(summary = "按分类统计", description = "按分类统计工单数量")
    @OperationLog(operation = "按分类统计工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    public Result<Map<String, Long>> byCategory() {
        log.info("按分类统计工单");
        Map<String, Long> result = statisticsService.countByCategory();
        return Result.success("查询成功", result);
    }

    @GetMapping("/by-status")
    @Operation(summary = "按状态统计", description = "按状态统计工单数量")
    @OperationLog(operation = "按状态统计工单", businessType = BusinessType.SELECT)
    @RequiresPermission("ticket:statistics:query")
    public Result<Map<String, Long>> byStatus() {
        log.info("按状态统计工单");
        Map<String, Long> result = statisticsService.countByStatus();
        return Result.success("查询成功", result);
    }
}
