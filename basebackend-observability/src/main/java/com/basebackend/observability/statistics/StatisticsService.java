package com.basebackend.observability.statistics;

import com.basebackend.observability.entity.JvmMetrics;
import com.basebackend.observability.entity.SlowSqlRecord;
import com.basebackend.observability.mapper.JvmMetricsMapper;
import com.basebackend.observability.mapper.SlowSqlRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计聚合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JvmMetricsMapper jvmMetricsMapper;
    private final SlowSqlRecordMapper slowSqlRecordMapper;

    /**
     * 获取系统健康总览
     */
    public Map<String, Object> getSystemHealthOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // 1. JVM健康状态
            String instanceId = getDefaultInstanceId();
            JvmMetrics latestMetrics = jvmMetricsMapper.selectLatest(instanceId);
            
            if (latestMetrics != null) {
                double heapUsagePercent = calculatePercentage(
                        latestMetrics.getHeapUsed(), latestMetrics.getHeapMax());
                
                Map<String, Object> jvmHealth = new HashMap<>();
                jvmHealth.put("heapUsagePercent", heapUsagePercent);
                jvmHealth.put("threadCount", latestMetrics.getThreadCount());
                jvmHealth.put("cpuUsage", latestMetrics.getCpuUsage());
                jvmHealth.put("gcCount", latestMetrics.getGcCount());
                jvmHealth.put("status", determineJvmStatus(heapUsagePercent, 
                        latestMetrics.getCpuUsage(), latestMetrics.getThreadCount()));
                
                overview.put("jvm", jvmHealth);
            }
            
            // 2. 慢SQL统计
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();
            List<SlowSqlRecord> slowSqls = slowSqlRecordMapper.selectByTimeRange(startTime, endTime);
            
            Map<String, Object> sqlStats = new HashMap<>();
            sqlStats.put("slowSqlCount", slowSqls.size());
            sqlStats.put("avgDuration", calculateAvgDuration(slowSqls));
            sqlStats.put("maxDuration", calculateMaxDuration(slowSqls));
            
            overview.put("sql", sqlStats);
            
            // 3. 总体健康分数
            int healthScore = calculateHealthScore(overview);
            overview.put("healthScore", healthScore);
            overview.put("healthStatus", getHealthStatus(healthScore));
            overview.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Failed to get system health overview", e);
        }
        
        return overview;
    }

    /**
     * 获取性能趋势
     */
    public Map<String, Object> getPerformanceTrend(int hours) {
        Map<String, Object> trend = new HashMap<>();
        
        try {
            String instanceId = getDefaultInstanceId();
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            LocalDateTime endTime = LocalDateTime.now();
            
            List<JvmMetrics> metrics = jvmMetricsMapper.selectByTimeRange(
                    instanceId, startTime, endTime);
            
            if (!metrics.isEmpty()) {
                // 堆内存趋势
                List<Map<String, Object>> heapTrend = metrics.stream()
                        .map(m -> {
                            Map<String, Object> point = new HashMap<>();
                            point.put("timestamp", m.getTimestamp());
                            point.put("used", m.getHeapUsed());
                            point.put("max", m.getHeapMax());
                            point.put("percent", calculatePercentage(m.getHeapUsed(), m.getHeapMax()));
                            return point;
                        })
                        .collect(Collectors.toList());
                trend.put("heapMemory", heapTrend);
                
                // CPU趋势
                List<Map<String, Object>> cpuTrend = metrics.stream()
                        .map(m -> {
                            Map<String, Object> point = new HashMap<>();
                            point.put("timestamp", m.getTimestamp());
                            point.put("usage", m.getCpuUsage());
                            return point;
                        })
                        .collect(Collectors.toList());
                trend.put("cpu", cpuTrend);
                
                // 线程数趋势
                List<Map<String, Object>> threadTrend = metrics.stream()
                        .map(m -> {
                            Map<String, Object> point = new HashMap<>();
                            point.put("timestamp", m.getTimestamp());
                            point.put("count", m.getThreadCount());
                            return point;
                        })
                        .collect(Collectors.toList());
                trend.put("threads", threadTrend);
                
                // GC趋势
                List<Map<String, Object>> gcTrend = metrics.stream()
                        .map(m -> {
                            Map<String, Object> point = new HashMap<>();
                            point.put("timestamp", m.getTimestamp());
                            point.put("count", m.getGcCount());
                            point.put("time", m.getGcTime());
                            return point;
                        })
                        .collect(Collectors.toList());
                trend.put("gc", gcTrend);
            }
            
        } catch (Exception e) {
            log.error("Failed to get performance trend", e);
        }
        
        return trend;
    }

    /**
     * 获取资源使用排行
     */
    public Map<String, Object> getResourceRanking() {
        Map<String, Object> ranking = new HashMap<>();
        
        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(24);
            LocalDateTime endTime = LocalDateTime.now();
            
            // Top慢SQL
            List<SlowSqlRecord> topSlowSqls = slowSqlRecordMapper.selectTopSlowSql(10);
            ranking.put("topSlowSql", topSlowSqls);
            
            // SQL方法聚合
            List<Map<String, Object>> sqlAggregation = 
                    slowSqlRecordMapper.aggregateByMethod(startTime, endTime);
            ranking.put("sqlByMethod", sqlAggregation);
            
        } catch (Exception e) {
            log.error("Failed to get resource ranking", e);
        }
        
        return ranking;
    }

    /**
     * 获取时段统计
     */
    public Map<String, Object> getTimeBasedStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 按小时统计
            Map<Integer, Integer> hourlyStats = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < 24; i++) {
                LocalDateTime start = now.minusHours(i + 1);
                LocalDateTime end = now.minusHours(i);
                
                List<SlowSqlRecord> records = slowSqlRecordMapper.selectByTimeRange(start, end);
                hourlyStats.put(i, records.size());
            }
            
            stats.put("hourlyCounts", hourlyStats);
            
        } catch (Exception e) {
            log.error("Failed to get time-based statistics", e);
        }
        
        return stats;
    }

    /**
     * 计算百分比
     */
    private double calculatePercentage(Long used, Long max) {
        if (max == null || max == 0) return 0;
        return (double) used / max * 100;
    }

    /**
     * 判断JVM状态
     */
    private String determineJvmStatus(double heapUsage, double cpuUsage, int threadCount) {
        if (heapUsage > 90 || cpuUsage > 80 || threadCount > 1000) {
            return "CRITICAL";
        } else if (heapUsage > 75 || cpuUsage > 60 || threadCount > 500) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * 计算平均耗时
     */
    private double calculateAvgDuration(List<SlowSqlRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream()
                .mapToLong(SlowSqlRecord::getDuration)
                .average()
                .orElse(0);
    }

    /**
     * 计算最大耗时
     */
    private long calculateMaxDuration(List<SlowSqlRecord> records) {
        if (records.isEmpty()) return 0;
        return records.stream()
                .mapToLong(SlowSqlRecord::getDuration)
                .max()
                .orElse(0);
    }

    /**
     * 计算健康分数
     */
    private int calculateHealthScore(Map<String, Object> overview) {
        int score = 100;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) overview.get("jvm");
        if (jvm != null) {
            Double heapUsage = (Double) jvm.get("heapUsagePercent");
            Double cpuUsage = (Double) jvm.get("cpuUsage");
            Integer threadCount = (Integer) jvm.get("threadCount");
            
            if (heapUsage != null && heapUsage > 90) score -= 30;
            else if (heapUsage != null && heapUsage > 75) score -= 15;
            
            if (cpuUsage != null && cpuUsage > 80) score -= 25;
            else if (cpuUsage != null && cpuUsage > 60) score -= 10;
            
            if (threadCount != null && threadCount > 1000) score -= 20;
            else if (threadCount != null && threadCount > 500) score -= 10;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sql = (Map<String, Object>) overview.get("sql");
        if (sql != null) {
            Integer slowSqlCount = (Integer) sql.get("slowSqlCount");
            if (slowSqlCount != null && slowSqlCount > 100) score -= 15;
            else if (slowSqlCount != null && slowSqlCount > 50) score -= 5;
        }
        
        return Math.max(0, score);
    }

    /**
     * 获取健康状态
     */
    private String getHealthStatus(int score) {
        if (score >= 80) return "HEALTHY";
        if (score >= 60) return "WARNING";
        return "CRITICAL";
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
