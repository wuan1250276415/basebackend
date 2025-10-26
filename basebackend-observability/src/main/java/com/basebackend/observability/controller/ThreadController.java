package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.profiling.model.DeadlockInfo;
import com.basebackend.observability.profiling.model.ThreadInfo;
import com.basebackend.observability.profiling.service.ThreadAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 线程分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/threads")
@RequiredArgsConstructor
@Tag(name = "线程分析", description = "线程监控、死锁检测和线程堆栈")
public class ThreadController {

    private final ThreadAnalysisService threadAnalysisService;

    @GetMapping
    @Operation(summary = "获取所有线程")
    public Result<List<ThreadInfo>> getAllThreads() {
        try {
            List<ThreadInfo> threads = threadAnalysisService.getAllThreads();
            return Result.success(threads);
        } catch (Exception e) {
            log.error("Failed to get threads", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/top-cpu")
    @Operation(summary = "获取CPU使用率最高的线程")
    public Result<List<ThreadInfo>> getTopCpuThreads(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ThreadInfo> threads = threadAnalysisService.getTopCpuThreads(limit);
            return Result.success(threads);
        } catch (Exception e) {
            log.error("Failed to get top CPU threads", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/top-blocked")
    @Operation(summary = "获取阻塞最多的线程")
    public Result<List<ThreadInfo>> getTopBlockedThreads(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ThreadInfo> threads = threadAnalysisService.getTopBlockedThreads(limit);
            return Result.success(threads);
        } catch (Exception e) {
            log.error("Failed to get top blocked threads", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/deadlocks")
    @Operation(summary = "检测死锁")
    public Result<List<DeadlockInfo>> detectDeadlocks() {
        try {
            List<DeadlockInfo> deadlocks = threadAnalysisService.detectDeadlocks();
            
            if (deadlocks.isEmpty()) {
                return Result.success("未检测到死锁", deadlocks);
            } else {
                Result<List<DeadlockInfo>> errorResult = Result.error("检测到 " + deadlocks.size() + " 个死锁");
                errorResult.setData(deadlocks);
                return errorResult;
            }
        } catch (Exception e) {
            log.error("Failed to detect deadlocks", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取线程统计")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = threadAnalysisService.getThreadStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get thread statistics", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/dump")
    @Operation(summary = "获取线程堆栈快照")
    public Result<Map<String, Object>> getThreadDump() {
        try {
            Map<String, Object> dump = threadAnalysisService.getThreadDump();
            return Result.success(dump);
        } catch (Exception e) {
            log.error("Failed to get thread dump", e);
            return Result.error(e.getMessage());
        }
    }
}
