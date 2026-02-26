package com.basebackend.gateway.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 网关流量统计收集器
 * <p>
 * 实时收集网关请求的流量数据，按路由/状态码/时间维度统计。
 * 数据存储在内存中，适用于单实例场景。集群环境建议替换为 Redis 实现。
 */
@Slf4j
@Component
public class GatewayMetricsCollector {

    /** 按路由统计 */
    private final Map<String, RouteMetrics> routeMetricsMap = new ConcurrentHashMap<>();

    /** 全局统计 */
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();

    /** 启动时间 */
    private final Instant startTime = Instant.now();

    /**
     * 记录一次请求
     *
     * @param routeId    路由 ID
     * @param statusCode HTTP 状态码
     * @param latencyMs  请求耗时（毫秒）
     * @param path       请求路径
     */
    public void record(String routeId, int statusCode, long latencyMs, String path) {
        totalRequests.increment();
        totalLatencyMs.add(latencyMs);

        if (statusCode >= 400) {
            totalErrors.increment();
        }

        String key = routeId != null ? routeId : "unknown";
        routeMetricsMap.computeIfAbsent(key, k -> new RouteMetrics(k))
                .record(statusCode, latencyMs);
    }

    /**
     * 获取全局概览
     */
    public Map<String, Object> getOverview() {
        long total = totalRequests.sum();
        long errors = totalErrors.sum();
        long latency = totalLatencyMs.sum();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("startTime", startTime.toString());
        overview.put("uptimeSeconds", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        overview.put("totalRequests", total);
        overview.put("totalErrors", errors);
        overview.put("errorRate", total > 0 ? String.format("%.2f%%", (double) errors / total * 100) : "0.00%");
        overview.put("avgLatencyMs", total > 0 ? latency / total : 0);
        overview.put("activeRoutes", routeMetricsMap.size());
        return overview;
    }

    /**
     * 获取按路由的统计数据
     */
    public List<Map<String, Object>> getRouteMetrics() {
        return routeMetricsMap.values().stream()
                .map(RouteMetrics::toMap)
                .sorted(Comparator.<Map<String, Object>, Long>comparing(m -> (Long) m.get("totalRequests")).reversed())
                .toList();
    }

    /**
     * 获取指定路由的统计数据
     */
    public Map<String, Object> getRouteMetrics(String routeId) {
        RouteMetrics metrics = routeMetricsMap.get(routeId);
        return metrics != null ? metrics.toMap() : Map.of("error", "路由不存在: " + routeId);
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        totalRequests.reset();
        totalErrors.reset();
        totalLatencyMs.reset();
        routeMetricsMap.clear();
        log.info("网关流量统计已重置");
    }

    // --- 路由级统计 ---

    private static class RouteMetrics {
        private final String routeId;
        private final LongAdder requests = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder totalLatencyMs = new LongAdder();
        private final AtomicLong maxLatencyMs = new AtomicLong(0);
        private final Map<Integer, LongAdder> statusCodeCounts = new ConcurrentHashMap<>();

        RouteMetrics(String routeId) {
            this.routeId = routeId;
        }

        void record(int statusCode, long latencyMs) {
            requests.increment();
            totalLatencyMs.add(latencyMs);
            maxLatencyMs.updateAndGet(current -> Math.max(current, latencyMs));

            if (statusCode >= 400) {
                errors.increment();
            }

            statusCodeCounts.computeIfAbsent(statusCode, k -> new LongAdder()).increment();
        }

        Map<String, Object> toMap() {
            long total = requests.sum();
            long err = errors.sum();
            long latency = totalLatencyMs.sum();

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("routeId", routeId);
            map.put("totalRequests", total);
            map.put("totalErrors", err);
            map.put("errorRate", total > 0 ? String.format("%.2f%%", (double) err / total * 100) : "0.00%");
            map.put("avgLatencyMs", total > 0 ? latency / total : 0);
            map.put("maxLatencyMs", maxLatencyMs.get());

            Map<String, Long> statusCodes = new LinkedHashMap<>();
            statusCodeCounts.forEach((code, count) -> statusCodes.put(String.valueOf(code), count.sum()));
            map.put("statusCodes", statusCodes);

            return map;
        }
    }
}
