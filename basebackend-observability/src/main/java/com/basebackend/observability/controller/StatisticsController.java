package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "系统性能统计和趋势分析")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/health-overview")
    @Operation(summary = "获取系统健康总览")
    public Result<Map<String, Object>> getHealthOverview() {
        try {
            Map<String, Object> overview = statisticsService.getSystemHealthOverview();
            return Result.success(overview);
        } catch (Exception e) {
            log.error("Failed to get health overview", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/performance-trend")
    @Operation(summary = "获取性能趋势")
    public Result<Map<String, Object>> getPerformanceTrend(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            Map<String, Object> trend = statisticsService.getPerformanceTrend(hours);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("Failed to get performance trend", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/resource-ranking")
    @Operation(summary = "获取资源使用排行")
    public Result<Map<String, Object>> getResourceRanking() {
        try {
            Map<String, Object> ranking = statisticsService.getResourceRanking();
            return Result.success(ranking);
        } catch (Exception e) {
            log.error("Failed to get resource ranking", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/time-based")
    @Operation(summary = "获取时段统计")
    public Result<Map<String, Object>> getTimeBasedStatistics() {
        try {
            Map<String, Object> stats = statisticsService.getTimeBasedStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get time-based statistics", e);
            return Result.error(e.getMessage());
        }
    }
}
