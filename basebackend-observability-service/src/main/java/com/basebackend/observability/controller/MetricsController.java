package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.dto.MetricsQueryRequest;
import com.basebackend.observability.service.MetricsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 指标查询控制器
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Validated
@Tag(name = "指标监控", description = "指标监控相关接口")
public class MetricsController {

    private final MetricsQueryService metricsQueryService;

    @PostMapping("/query")
    @Operation(summary = "查询指标数据")
    public Result<Map<String, Object>> queryMetrics(@RequestBody MetricsQueryRequest request) {
        log.info("Querying metrics: {}", request.getMetricName());
        Map<String, Object> result = metricsQueryService.queryMetrics(request);
        return Result.success(result);
    }

    @GetMapping("/available")
    @Operation(summary = "获取所有可用指标")
    public Result<List<String>> getAvailableMetrics() {
        List<String> metrics = metricsQueryService.getAvailableMetrics();
        return Result.success(metrics);
    }

    @GetMapping("/overview")
    @Operation(summary = "获取系统概览")
    public Result<Map<String, Object>> getSystemOverview() {
        Map<String, Object> overview = metricsQueryService.getSystemOverview();
        return Result.success(overview);
    }
}
