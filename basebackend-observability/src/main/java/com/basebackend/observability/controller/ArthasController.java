package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.arthas.ArthasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Arthas调试控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/arthas")
@RequiredArgsConstructor
@Tag(name = "Arthas调试", description = "Arthas实时调试和诊断工具")
public class ArthasController {

    private final ArthasService arthasService;

    @PostMapping("/start")
    @Operation(summary = "启动Arthas")
    public Result<Map<String, Object>> startArthas(
            @RequestParam(required = false) Integer port) {
        try {
            Map<String, Object> result = arthasService.startArthas(port);
            if ("success".equals(result.get("status"))) {
                return Result.success(result);
            } else {
                Result<Map<String, Object>> errorResult = Result.error(result.get("message").toString());
                errorResult.setData(result);
                return errorResult;
            }
        } catch (Exception e) {
            log.error("Failed to start Arthas", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/stop")
    @Operation(summary = "停止Arthas")
    public Result<Map<String, Object>> stopArthas() {
        try {
            Map<String, Object> result = arthasService.stopArthas();
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to stop Arthas", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取Arthas状态")
    public Result<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = arthasService.getStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("Failed to get Arthas status", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/execute")
    @Operation(summary = "执行Arthas命令")
    public Result<String> executeCommand(@RequestParam String command) {
        try {
            String result = arthasService.executeCommand(command);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to execute Arthas command", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/thread")
    @Operation(summary = "查看线程信息")
    public Result<String> thread(
            @RequestParam(required = false) Integer threadId,
            @RequestParam(required = false) Integer lines) {
        try {
            String result = arthasService.thread(threadId, lines);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get thread info", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "查看JVM Dashboard")
    public Result<String> dashboard() {
        try {
            String result = arthasService.dashboard();
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get dashboard", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/jad")
    @Operation(summary = "反编译类")
    public Result<String> jad(@RequestParam String className) {
        try {
            String result = arthasService.jad(className);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to decompile class", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/watch")
    @Operation(summary = "监控方法")
    public Result<String> watch(
            @RequestParam String className,
            @RequestParam String methodName,
            @RequestParam(required = false) String express) {
        try {
            String result = arthasService.watch(className, methodName, express);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to watch method", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/trace")
    @Operation(summary = "追踪方法调用")
    public Result<String> trace(
            @RequestParam String className,
            @RequestParam String methodName) {
        try {
            String result = arthasService.trace(className, methodName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to trace method", e);
            return Result.error(e.getMessage());
        }
    }
}
