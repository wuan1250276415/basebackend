package com.basebackend.gateway.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 网关仪表盘 API
 * <p>
 * 提供网关流量概览、路由级统计、健康状态等管理接口。
 */
@RestController
@RequestMapping("/actuator/gateway/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GatewayMetricsCollector metricsCollector;

    /** 全局概览 */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> overview() {
        return Mono.just(ResponseEntity.ok(metricsCollector.getOverview()));
    }

    /** 所有路由统计（按请求量排序） */
    @GetMapping("/routes")
    public Mono<ResponseEntity<List<Map<String, Object>>>> routeMetrics() {
        return Mono.just(ResponseEntity.ok(metricsCollector.getRouteMetrics()));
    }

    /** 指定路由统计 */
    @GetMapping("/routes/{routeId}")
    public Mono<ResponseEntity<Map<String, Object>>> routeMetrics(@PathVariable String routeId) {
        return Mono.just(ResponseEntity.ok(metricsCollector.getRouteMetrics(routeId)));
    }

    /** 重置统计数据 */
    @PostMapping("/reset")
    public Mono<ResponseEntity<String>> reset() {
        metricsCollector.reset();
        return Mono.just(ResponseEntity.ok("流量统计已重置"));
    }
}
