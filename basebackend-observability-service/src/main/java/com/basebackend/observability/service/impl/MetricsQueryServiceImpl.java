package com.basebackend.observability.service.impl;

import com.basebackend.observability.dto.MetricsQueryRequest;
import com.basebackend.observability.service.MetricsQueryService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标查询服务实现
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsQueryServiceImpl implements MetricsQueryService {

    private final MeterRegistry meterRegistry;

    @Override
    public Map<String, Object> queryMetrics(MetricsQueryRequest request) {
        log.info("Querying metrics: {}", request.getMetricName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("metricName", request.getMetricName());
        result.put("startTime", request.getStartTime());
        result.put("endTime", request.getEndTime());
        
        // 从Micrometer获取指标数据
        try {
            meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(request.getMetricName()))
                .findFirst()
                .ifPresent(meter -> {
                    List<Map<String, Object>> dataPoints = new ArrayList<>();
                    meter.measure().forEach(measurement -> {
                        Map<String, Object> point = new HashMap<>();
                        point.put("statistic", measurement.getStatistic().name());
                        point.put("value", measurement.getValue());
                        dataPoints.add(point);
                    });
                    result.put("data", dataPoints);
                });
        } catch (Exception e) {
            log.error("Error querying metrics", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public List<String> getAvailableMetrics() {
        log.info("Getting available metrics");
        
        return meterRegistry.getMeters().stream()
            .map(meter -> meter.getId().getName())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSystemOverview() {
        log.info("Getting system overview");
        
        Map<String, Object> overview = new HashMap<>();
        
        // JVM内存
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());
        overview.put("memory", memory);
        
        // 系统信息
        Map<String, Object> system = new HashMap<>();
        system.put("processors", runtime.availableProcessors());
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("javaVersion", System.getProperty("java.version"));
        overview.put("system", system);
        
        // 指标数量
        overview.put("metricsCount", meterRegistry.getMeters().size());
        
        return overview;
    }
}
