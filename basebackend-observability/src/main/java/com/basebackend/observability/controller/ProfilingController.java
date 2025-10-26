package com.basebackend.observability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.model.Result;
import com.basebackend.observability.entity.JvmMetrics;
import com.basebackend.observability.entity.SlowSqlRecord;
import com.basebackend.observability.mapper.JvmMetricsMapper;
import com.basebackend.observability.mapper.SlowSqlRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/profiling")
@RequiredArgsConstructor
@Tag(name = "性能分析", description = "JVM监控和SQL性能分析")
public class ProfilingController {

    private final JvmMetricsMapper jvmMetricsMapper;
    private final SlowSqlRecordMapper slowSqlRecordMapper;

    @GetMapping("/jvm/metrics")
    @Operation(summary = "获取最新JVM指标")
    public Result<Map<String, Object>> getJvmMetrics(
            @RequestParam(required = false) String instanceId) {
        try {
            if (instanceId == null) {
                instanceId = getDefaultInstanceId();
            }
            
            JvmMetrics latest = jvmMetricsMapper.selectLatest(instanceId);
            
            if (latest == null) {
                return Result.error("No metrics found for instance: " + instanceId);
            }
            
            // 计算使用率
            Map<String, Object> result = new HashMap<>();
            result.put("metrics", latest);
            result.put("heapUsagePercent", calculatePercentage(latest.getHeapUsed(), latest.getHeapMax()));
            result.put("timestamp", latest.getTimestamp());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("Failed to get JVM metrics", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/jvm/history")
    @Operation(summary = "获取JVM指标历史")
    public Result<List<JvmMetrics>> getJvmHistory(
            @RequestParam(required = false) String instanceId,
            @RequestParam(defaultValue = "1") int hours) {
        try {
            if (instanceId == null) {
                instanceId = getDefaultInstanceId();
            }
            
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            LocalDateTime endTime = LocalDateTime.now();
            
            List<JvmMetrics> history = jvmMetricsMapper.selectByTimeRange(
                    instanceId, startTime, endTime);
            
            return Result.success(history);
            
        } catch (Exception e) {
            log.error("Failed to get JVM history", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/sql/slow")
    @Operation(summary = "获取慢SQL列表")
    public Result<List<SlowSqlRecord>> getSlowSql(
            @RequestParam(defaultValue = "1") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            LocalDateTime endTime = LocalDateTime.now();
            
            List<SlowSqlRecord> slowSqls = slowSqlRecordMapper.selectByTimeRange(startTime, endTime);
            
            // 限制返回数量
            if (slowSqls.size() > limit) {
                slowSqls = slowSqls.subList(0, limit);
            }
            
            return Result.success(slowSqls);
            
        } catch (Exception e) {
            log.error("Failed to get slow SQL", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/sql/top")
    @Operation(summary = "获取Top N慢SQL")
    public Result<List<SlowSqlRecord>> getTopSlowSql(
            @RequestParam(defaultValue = "10") int topN) {
        try {
            List<SlowSqlRecord> topSlowSqls = slowSqlRecordMapper.selectTopSlowSql(topN);
            return Result.success(topSlowSqls);
        } catch (Exception e) {
            log.error("Failed to get top slow SQL", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/sql/aggregate")
    @Operation(summary = "慢SQL聚合统计")
    public Result<List<Map<String, Object>>> aggregateSlowSql(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            LocalDateTime endTime = LocalDateTime.now();
            
            List<Map<String, Object>> aggregated = slowSqlRecordMapper.aggregateByMethod(startTime, endTime);
            return Result.success(aggregated);
        } catch (Exception e) {
            log.error("Failed to aggregate slow SQL", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 计算百分比
     */
    private double calculatePercentage(Long used, Long max) {
        if (max == null || max == 0) return 0;
        return (double) used / max * 100;
    }

    /**
     * 获取默认实例ID
     */
    private String getDefaultInstanceId() {
        String hostName = System.getenv("HOSTNAME");
        if (hostName == null) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostName = "localhost";
            }
        }
        return hostName;
    }
}
