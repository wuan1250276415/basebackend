package com.basebackend.gateway.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GatewayMetricsCollector 测试")
class GatewayMetricsCollectorTest {

    private GatewayMetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new GatewayMetricsCollector();
    }

    @Test
    @DisplayName("记录请求并获取概览")
    void recordAndOverview() {
        collector.record("route-user", 200, 50, "/api/users");
        collector.record("route-user", 200, 100, "/api/users/1");
        collector.record("route-order", 500, 200, "/api/orders");

        Map<String, Object> overview = collector.getOverview();
        assertThat(overview.get("totalRequests")).isEqualTo(3L);
        assertThat(overview.get("totalErrors")).isEqualTo(1L);
        assertThat(overview.get("activeRoutes")).isEqualTo(2);
    }

    @Test
    @DisplayName("错误率计算正确")
    void errorRateCalculation() {
        collector.record("r1", 200, 10, "/ok");
        collector.record("r1", 200, 10, "/ok");
        collector.record("r1", 500, 10, "/err");
        collector.record("r1", 404, 10, "/notfound");

        Map<String, Object> overview = collector.getOverview();
        assertThat(overview.get("totalRequests")).isEqualTo(4L);
        assertThat(overview.get("totalErrors")).isEqualTo(2L);
        assertThat((String) overview.get("errorRate")).isEqualTo("50.00%");
    }

    @Test
    @DisplayName("按路由统计")
    void routeMetrics() {
        collector.record("route-a", 200, 50, "/a");
        collector.record("route-a", 200, 100, "/a");
        collector.record("route-b", 200, 30, "/b");

        List<Map<String, Object>> routes = collector.getRouteMetrics();
        assertThat(routes).hasSize(2);
        // 按请求量排序，route-a (2次) 排在前面
        assertThat(routes.get(0).get("routeId")).isEqualTo("route-a");
        assertThat(routes.get(0).get("totalRequests")).isEqualTo(2L);
        assertThat(routes.get(1).get("routeId")).isEqualTo("route-b");
    }

    @Test
    @DisplayName("路由级状态码分布")
    void routeStatusCodes() {
        collector.record("r1", 200, 10, "/ok");
        collector.record("r1", 200, 10, "/ok");
        collector.record("r1", 404, 10, "/miss");
        collector.record("r1", 500, 10, "/err");

        Map<String, Object> routeMetrics = collector.getRouteMetrics("r1");
        @SuppressWarnings("unchecked")
        Map<String, Long> statusCodes = (Map<String, Long>) routeMetrics.get("statusCodes");
        assertThat(statusCodes.get("200")).isEqualTo(2L);
        assertThat(statusCodes.get("404")).isEqualTo(1L);
        assertThat(statusCodes.get("500")).isEqualTo(1L);
    }

    @Test
    @DisplayName("最大延迟统计")
    void maxLatency() {
        collector.record("r1", 200, 50, "/a");
        collector.record("r1", 200, 200, "/b");
        collector.record("r1", 200, 100, "/c");

        Map<String, Object> metrics = collector.getRouteMetrics("r1");
        assertThat(metrics.get("maxLatencyMs")).isEqualTo(200L);
    }

    @Test
    @DisplayName("查询不存在的路由")
    void routeNotFound() {
        Map<String, Object> metrics = collector.getRouteMetrics("nonexistent");
        assertThat(metrics).containsKey("error");
    }

    @Test
    @DisplayName("reset 重置所有数据")
    void reset() {
        collector.record("r1", 200, 50, "/a");
        collector.record("r2", 500, 100, "/b");
        collector.reset();

        Map<String, Object> overview = collector.getOverview();
        assertThat(overview.get("totalRequests")).isEqualTo(0L);
        assertThat(overview.get("totalErrors")).isEqualTo(0L);
        assertThat(overview.get("activeRoutes")).isEqualTo(0);
    }

    @Test
    @DisplayName("空数据时概览正常")
    void emptyOverview() {
        Map<String, Object> overview = collector.getOverview();
        assertThat(overview.get("totalRequests")).isEqualTo(0L);
        assertThat(overview.get("errorRate")).isEqualTo("0.00%");
        assertThat(overview.get("avgLatencyMs")).isEqualTo(0L);
    }

    @Test
    @DisplayName("null routeId 归入 unknown")
    void nullRouteId() {
        collector.record(null, 200, 50, "/test");

        Map<String, Object> metrics = collector.getRouteMetrics("unknown");
        assertThat(metrics.get("totalRequests")).isEqualTo(1L);
    }
}
