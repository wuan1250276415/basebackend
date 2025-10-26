package com.basebackend.observability.gc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * GC分析服务
 */
@Slf4j
@Service
public class GcAnalysisService {

    private final Map<String, GcSnapshot> previousSnapshots = new HashMap<>();

    /**
     * 获取GC统计信息
     */
    public Map<String, Object> getGcStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Map<String, Object>> collectors = new ArrayList<>();
        long totalGcCount = 0;
        long totalGcTime = 0;
        
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> collectorInfo = new HashMap<>();
            
            String name = gc.getName();
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            
            collectorInfo.put("name", name);
            collectorInfo.put("count", count);
            collectorInfo.put("time", time);
            
            // 计算频率
            GcSnapshot previous = previousSnapshots.get(name);
            if (previous != null) {
                long countDiff = count - previous.count;
                long timeDiff = System.currentTimeMillis() - previous.timestamp;
                
                double frequency = timeDiff > 0 ? (double) countDiff / (timeDiff / 60000.0) : 0;
                collectorInfo.put("frequency", frequency); // 次/分钟
                
                long avgPause = countDiff > 0 ? (time - previous.time) / countDiff : 0;
                collectorInfo.put("avgPause", avgPause);
            }
            
            // 更新快照
            previousSnapshots.put(name, new GcSnapshot(count, time, System.currentTimeMillis()));
            
            collectors.add(collectorInfo);
            totalGcCount += count;
            totalGcTime += time;
        }
        
        stats.put("collectors", collectors);
        stats.put("totalGcCount", totalGcCount);
        stats.put("totalGcTime", totalGcTime);
        stats.put("timestamp", System.currentTimeMillis());
        
        // 分析GC问题
        List<String> issues = analyzeGcIssues(collectors);
        stats.put("issues", issues);
        
        return stats;
    }

    /**
     * 分析GC问题
     */
    private List<String> analyzeGcIssues(List<Map<String, Object>> collectors) {
        List<String> issues = new ArrayList<>();
        
        for (Map<String, Object> collector : collectors) {
            String name = (String) collector.get("name");
            
            // 检查频率
            if (collector.containsKey("frequency")) {
                Double frequency = (Double) collector.get("frequency");
                if (frequency != null && frequency > 10) {
                    issues.add(String.format("%s GC频率过高: %.2f 次/分钟", name, frequency));
                }
            }
            
            // 检查平均暂停时间
            if (collector.containsKey("avgPause")) {
                Long avgPause = (Long) collector.get("avgPause");
                if (avgPause != null && avgPause > 100) {
                    issues.add(String.format("%s GC平均暂停时间过长: %d ms", name, avgPause));
                }
            }
        }
        
        return issues;
    }

    /**
     * 获取GC趋势分析
     */
    public Map<String, Object> getGcTrend() {
        Map<String, Object> trend = new HashMap<>();
        
        // 获取当前GC信息
        Map<String, Object> current = getGcStatistics();
        trend.put("current", current);
        
        // 添加建议
        @SuppressWarnings("unchecked")
        List<String> issues = (List<String>) current.get("issues");
        List<String> suggestions = generateSuggestions(issues);
        trend.put("suggestions", suggestions);
        
        return trend;
    }

    /**
     * 生成优化建议
     */
    private List<String> generateSuggestions(List<String> issues) {
        List<String> suggestions = new ArrayList<>();
        
        if (issues.isEmpty()) {
            suggestions.add("GC运行正常，无需优化");
            return suggestions;
        }
        
        for (String issue : issues) {
            if (issue.contains("频率过高")) {
                suggestions.add("考虑增加堆内存大小或优化对象创建");
                suggestions.add("检查是否存在内存泄漏");
                suggestions.add("考虑调整新生代和老年代比例");
            }
            
            if (issue.contains("暂停时间过长")) {
                suggestions.add("考虑使用G1或ZGC等低延迟垃圾收集器");
                suggestions.add("调整GC线程数");
                suggestions.add("优化大对象的分配");
            }
        }
        
        return suggestions.stream().distinct().toList();
    }

    /**
     * GC快照
     */
    private static class GcSnapshot {
        long count;
        long time;
        long timestamp;
        
        GcSnapshot(long count, long time, long timestamp) {
            this.count = count;
            this.time = time;
            this.timestamp = timestamp;
        }
    }
}
