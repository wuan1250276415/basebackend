package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.gc.GcAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GC分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/gc")
@RequiredArgsConstructor
@Tag(name = "GC分析", description = "垃圾回收分析和优化建议")
public class GcController {

    private final GcAnalysisService gcAnalysisService;

    @GetMapping("/statistics")
    @Operation(summary = "获取GC统计信息")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = gcAnalysisService.getGcStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get GC statistics", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/trend")
    @Operation(summary = "获取GC趋势分析")
    public Result<Map<String, Object>> getTrend() {
        try {
            Map<String, Object> trend = gcAnalysisService.getGcTrend();
            return Result.success(trend);
        } catch (Exception e) {
            log.error("Failed to get GC trend", e);
            return Result.error(e.getMessage());
        }
    }
}
