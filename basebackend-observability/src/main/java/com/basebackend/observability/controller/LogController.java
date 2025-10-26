package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.entity.ExceptionAggregation;
import com.basebackend.observability.logging.model.LogSearchQuery;
import com.basebackend.observability.logging.model.LogSearchResult;
import com.basebackend.observability.logging.service.ElasticsearchLogService;
import com.basebackend.observability.logging.service.ExceptionAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/logs")
@RequiredArgsConstructor
@Tag(name = "日志分析", description = "日志搜索、异常聚合和实时日志流")
public class LogController {

    private final ElasticsearchLogService logService;
    private final ExceptionAggregationService exceptionService;

    @PostMapping("/search")
    @Operation(summary = "搜索日志")
    public Result<LogSearchResult> searchLogs(@RequestBody LogSearchQuery query) {
        try {
            LogSearchResult result = logService.search(query);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to search logs", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/context/{logId}")
    @Operation(summary = "获取日志上下文")
    public Result<LogSearchResult> getLogContext(
            @PathVariable String logId,
            @RequestParam(defaultValue = "10") int before,
            @RequestParam(defaultValue = "10") int after) {
        try {
            LogSearchResult result = logService.getContext(logId, before, after);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get log context", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/exceptions/top")
    @Operation(summary = "获取Top异常")
    public Result<List<ExceptionAggregation>> getTopExceptions(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<ExceptionAggregation> exceptions = exceptionService.getTopExceptions(limit, hours);
            return Result.success(exceptions);
        } catch (Exception e) {
            log.error("Failed to get top exceptions", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/exceptions/{id}/status")
    @Operation(summary = "更新异常状态")
    public Result<Void> updateExceptionStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            exceptionService.updateExceptionStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to update exception status", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/exceptions/record")
    @Operation(summary = "记录异常")
    public Result<Void> recordException(
            @RequestParam String exceptionClass,
            @RequestParam String exceptionMessage,
            @RequestParam String stackTrace,
            @RequestParam String serviceName,
            @RequestParam(required = false) String logId) {
        try {
            exceptionService.recordException(
                    exceptionClass, exceptionMessage, stackTrace, serviceName, logId);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to record exception", e);
            return Result.error(e.getMessage());
        }
    }
}
