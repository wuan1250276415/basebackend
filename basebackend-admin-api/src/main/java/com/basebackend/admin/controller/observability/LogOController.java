package com.basebackend.admin.controller.observability;

import com.basebackend.admin.dto.observability.LogQueryRequest;
import com.basebackend.admin.service.observability.LogQueryService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "可观测性-日志", description = "日志查询相关接口")
public class LogOController {

    private final LogQueryService logQueryService;

    @PostMapping("/query")
    @Operation(summary = "查询日志")
    public Result<Map<String, Object>> queryLogs(@RequestBody LogQueryRequest request) {
        log.info("Querying logs with keyword: {}", request.getKeyword());
        Map<String, Object> result = logQueryService.queryLogs(request);
        return Result.success(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取日志统计")
    public Result<Map<String, Object>> getLogStats(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        Map<String, Object> stats = logQueryService.getLogStats(startTime, endTime);
        return Result.success(stats);
    }

    @GetMapping("/trace/{traceId}")
    @Operation(summary = "根据TraceId查询日志")
    public Result<List<Map<String, Object>>> getLogsByTraceId(@PathVariable String traceId) {
        log.info("Querying logs by traceId: {}", traceId);
        List<Map<String, Object>> logs = logQueryService.queryLogsByTraceId(traceId);
        return Result.success(logs);
    }
}
