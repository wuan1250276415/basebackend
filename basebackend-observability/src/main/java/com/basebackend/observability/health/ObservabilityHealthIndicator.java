package com.basebackend.observability.health;

import com.basebackend.observability.entity.JvmMetrics;
import com.basebackend.observability.mapper.JvmMetricsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 可观测性健康指示器
 */
@Component
@RequiredArgsConstructor
public class ObservabilityHealthIndicator implements HealthIndicator {

    private final JvmMetricsMapper jvmMetricsMapper;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // 检查JVM监控是否正常
            String instanceId = getDefaultInstanceId();
            JvmMetrics latest = jvmMetricsMapper.selectLatest(instanceId);
            
            if (latest == null) {
                details.put("jvmMonitoring", "NOT_AVAILABLE");
                details.put("message", "No JVM metrics found");
                return Health.down().withDetails(details).build();
            }
            
            // 检查数据是否新鲜（5分钟内）
            long age = System.currentTimeMillis() - 
                    latest.getTimestamp().atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli();
            
            if (age > 300000) { // 5分钟
                details.put("jvmMonitoring", "STALE");
                details.put("dataAge", age / 1000 + " seconds");
                return Health.down().withDetails(details).build();
            }
            
            // 检查JVM健康状况
            double heapUsage = calculatePercentage(latest.getHeapUsed(), latest.getHeapMax());
            details.put("heapUsagePercent", heapUsage);
            details.put("threadCount", latest.getThreadCount());
            details.put("cpuUsage", latest.getCpuUsage());
            details.put("dataAge", age / 1000 + " seconds");
            
            if (heapUsage > 95 || latest.getCpuUsage() > 90) {
                details.put("status", "CRITICAL");
                return Health.down().withDetails(details).build();
            } else if (heapUsage > 85 || latest.getCpuUsage() > 75) {
                details.put("status", "WARNING");
                return Health.up().withDetails(details).build();
            }
            
            details.put("status", "HEALTHY");
            return Health.up().withDetails(details).build();
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());
            return Health.down().withDetails(details).build();
        }
    }

    private double calculatePercentage(Long used, Long max) {
        if (max == null || max == 0) return 0;
        return (double) used / max * 100;
    }

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
