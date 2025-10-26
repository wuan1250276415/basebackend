package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.entity.TraceSpanExt;
import com.basebackend.observability.mapper.TraceSpanExtMapper;
import com.basebackend.observability.trace.model.Bottleneck;
import com.basebackend.observability.trace.model.TraceGraph;
import com.basebackend.observability.trace.service.PerformanceBottleneckDetector;
import com.basebackend.observability.trace.service.TraceVisualizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 追踪控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/traces")
@RequiredArgsConstructor
@Tag(name = "分布式追踪", description = "调用链分析和性能瓶颈检测")
public class TraceController {

    private final TraceSpanExtMapper traceSpanExtMapper;
    private final TraceVisualizationService visualizationService;
    private final PerformanceBottleneckDetector bottleneckDetector;

    @GetMapping("/{traceId}/graph")
    @Operation(summary = "获取调用链可视化图")
    public Result<TraceGraph> getTraceGraph(@PathVariable String traceId) {
        log.info("Getting trace graph for traceId: {}", traceId);
        
        try {
            // 1. 查询Span数据
            List<TraceSpanExt> spans = traceSpanExtMapper.selectByTraceId(traceId);
            
            if (spans.isEmpty()) {
                return Result.error("Trace not found: " + traceId);
            }
            
            // 2. 构建调用图
            TraceGraph graph = visualizationService.getTraceGraph(traceId, spans);
            
            return Result.success("查询成功", graph);
            
        } catch (Exception e) {
            log.error("Failed to get trace graph", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{traceId}/bottlenecks")
    @Operation(summary = "检测性能瓶颈")
    public Result<List<Bottleneck>> detectBottlenecks(@PathVariable String traceId) {
        log.info("Detecting bottlenecks for traceId: {}", traceId);
        
        try {
            // 1. 获取调用图
            List<TraceSpanExt> spans = traceSpanExtMapper.selectByTraceId(traceId);
            TraceGraph graph = visualizationService.getTraceGraph(traceId, spans);
            
            // 2. 检测瓶颈
            List<Bottleneck> bottlenecks = bottleneckDetector.detectBottlenecks(graph);
            
            return Result.success("检测完成", bottlenecks);
            
        } catch (Exception e) {
            log.error("Failed to detect bottlenecks", e);
            return Result.error("检测失败: " + e.getMessage());
        }
    }

    @GetMapping("/{traceId}/spans")
    @Operation(summary = "获取Span列表")
    public Result<List<TraceSpanExt>> getSpans(@PathVariable String traceId) {
        try {
            List<TraceSpanExt> spans = traceSpanExtMapper.selectByTraceId(traceId);
            return Result.success(spans);
        } catch (Exception e) {
            log.error("Failed to get spans", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/errors")
    @Operation(summary = "获取错误追踪列表")
    public Result<List<TraceSpanExt>> getErrorTraces(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        try {
            List<TraceSpanExt> errorSpans = traceSpanExtMapper.selectErrorSpans(startTime, endTime);
            return Result.success(errorSpans);
        } catch (Exception e) {
            log.error("Failed to get error traces", e);
            return Result.error(e.getMessage());
        }
    }
}
