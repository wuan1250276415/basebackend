package com.basebackend.database.controller;

import com.basebackend.database.interceptor.SlowSqlInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 慢查询监控管理接口
 *
 * 提供慢查询统计信息查询和管理功能
 *
 * @author 浮浮酱
 */
@Slf4j
@RestController
@RequestMapping("/api/database/slow-sql")
@Tag(name = "慢查询监控", description = "慢查询统计信息查询和管理")
@ConditionalOnProperty(name = "mybatis.slow-sql-monitor.enabled", havingValue = "true", matchIfMissing = true)
public class SlowSqlMonitorController {

    /**
     * 获取慢查询 TOP N
     *
     * @param topN 返回前 N 条慢查询，默认 10
     * @return 慢查询统计列表
     */
    @GetMapping("/top")
    @Operation(summary = "获取慢查询 TOP N", description = "返回执行时间最长的前 N 条慢查询统计信息")
    public Map<String, Object> getTopSlowSql(
            @Parameter(description = "返回前 N 条，默认 10")
            @RequestParam(defaultValue = "10") int topN
    ) {
        log.info("查询慢查询 TOP {}", topN);

        List<SlowSqlInterceptor.SlowSqlStatistics> statistics = SlowSqlInterceptor.getTopSlowSql(topN);

        Map<String, Object> result = new HashMap<>();
        result.put("total", statistics.size());
        result.put("topN", topN);
        result.put("data", statistics);

        return result;
    }

    /**
     * 获取慢查询详细统计信息
     *
     * @return 所有慢查询的详细统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取慢查询详细统计", description = "返回所有慢查询的详细执行统计信息")
    public Map<String, Object> getSlowSqlStatistics() {
        log.info("查询慢查询详细统计信息");

        List<SlowSqlInterceptor.SlowSqlStatistics> statistics = SlowSqlInterceptor.getTopSlowSql(Integer.MAX_VALUE);

        // 计算汇总信息
        long totalExecutionCount = statistics.stream()
                .mapToLong(SlowSqlInterceptor.SlowSqlStatistics::getExecutionCount)
                .sum();

        long totalExecutionTime = statistics.stream()
                .mapToLong(SlowSqlInterceptor.SlowSqlStatistics::getTotalTime)
                .sum();

        long averageExecutionTime = statistics.isEmpty() ? 0 : totalExecutionTime / statistics.size();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSqlCount", statistics.size());
        summary.put("totalExecutionCount", totalExecutionCount);
        summary.put("totalExecutionTime", totalExecutionTime + " ms");
        summary.put("averageExecutionTime", averageExecutionTime + " ms");

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("details", statistics);

        return result;
    }

    /**
     * 清除慢查询统计
     *
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清除慢查询统计", description = "清除所有慢查询统计信息（慎用）")
    public Map<String, Object> clearStatistics() {
        log.warn("清除慢查询统计信息");

        SlowSqlInterceptor.clearStatistics();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "慢查询统计已清除");

        return result;
    }

    /**
     * 健康检查
     *
     * @return 监控状态
     */
    @GetMapping("/health")
    @Operation(summary = "监控健康检查", description = "检查慢查询监控是否正常运行")
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("component", "slow-sql-monitor");
        result.put("statisticsCount", SlowSqlInterceptor.getTopSlowSql(Integer.MAX_VALUE).size());

        return result;
    }
}
