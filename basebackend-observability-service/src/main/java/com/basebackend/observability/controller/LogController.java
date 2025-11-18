package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.dto.LogQueryRequest;
import com.basebackend.observability.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 日志查询控制器
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "日志查询", description = "日志查询相关接口")
public class LogController {

    private final LogQueryService logQueryService;

    @PostMapping("/search")
    @Operation(summary = "搜索日志")
    public Result<Map<String, Object>> searchLogs(@RequestBody LogQueryRequest request) {
        log.info("Searching logs: service={}, level={}", request.getServiceName(), request.getLevel());
        Map<String, Object> result = logQueryService.searchLogs(request);
        return Result.success(result);
    }

    @GetMapping("/services")
    @Operation(summary = "获取服务列表")
    public Result<List<String>> getServices() {
        List<String> services = logQueryService.getServices();
        return Result.success(services);
    }

    @GetMapping("/levels")
    @Operation(summary = "获取日志级别列表")
    public Result<List<String>> getLogLevels() {
        List<String> levels = logQueryService.getLogLevels();
        return Result.success(levels);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取日志统计")
    public Result<Map<String, Object>> getLogStats(
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "1") int hours) {
        Map<String, Object> stats = logQueryService.getLogStats(serviceName, hours);
        return Result.success(stats);
    }

    @GetMapping("/tail")
    @Operation(summary = "实时日志流")
    public Result<List<Map<String, Object>>> tailLogs(
            @RequestParam String serviceName,
            @RequestParam(defaultValue = "100") int lines) {
        List<Map<String, Object>> logs = logQueryService.tailLogs(serviceName, lines);
        return Result.success(logs);
    }
}
