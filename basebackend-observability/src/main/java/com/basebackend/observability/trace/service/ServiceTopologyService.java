package com.basebackend.observability.trace.service;

import com.basebackend.observability.entity.ServiceDependency;
import com.basebackend.observability.mapper.ServiceDependencyMapper;
import com.basebackend.observability.trace.model.ServiceEdge;
import com.basebackend.observability.trace.model.ServiceNode;
import com.basebackend.observability.trace.model.ServiceTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 服务拓扑生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTopologyService {

    private final ServiceDependencyMapper dependencyMapper;

    /**
     * 生成服务拓扑图
     */
    public ServiceTopology generateTopology(LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 查询依赖关系
        List<ServiceDependency> dependencies = dependencyMapper.selectByTimeRange(startTime, endTime);
        
        if (dependencies.isEmpty()) {
            return ServiceTopology.builder()
                    .nodes(Collections.emptyList())
                    .edges(Collections.emptyList())
                    .statistics(new HashMap<>())
                    .build();
        }

        // 2. 聚合服务节点数据
        Map<String, NodeData> nodeDataMap = aggregateNodeData(dependencies);
        
        // 3. 构建节点
        List<ServiceNode> nodes = buildNodes(nodeDataMap);
        
        // 4. 构建边
        List<ServiceEdge> edges = buildEdges(dependencies);
        
        // 5. 计算统计信息
        Map<String, Object> statistics = calculateStatistics(nodes, edges);
        
        return ServiceTopology.builder()
                .nodes(nodes)
                .edges(edges)
                .statistics(statistics)
                .build();
    }

    /**
     * 聚合节点数据
     */
    private Map<String, NodeData> aggregateNodeData(List<ServiceDependency> dependencies) {
        Map<String, NodeData> nodeDataMap = new HashMap<>();
        
        for (ServiceDependency dep : dependencies) {
            // 聚合源服务
            NodeData sourceData = nodeDataMap.computeIfAbsent(
                    dep.getFromService(), k -> new NodeData());
            sourceData.callCount += dep.getCallCount();
            sourceData.errorCount += dep.getErrorCount();
            sourceData.totalDuration += dep.getTotalDuration();
            sourceData.durations.add(dep.getTotalDuration().doubleValue() / dep.getCallCount());
            
            // 聚合目标服务
            NodeData targetData = nodeDataMap.computeIfAbsent(
                    dep.getToService(), k -> new NodeData());
            targetData.callCount += dep.getCallCount();
            targetData.errorCount += dep.getErrorCount();
            targetData.totalDuration += dep.getTotalDuration();
            targetData.durations.add(dep.getTotalDuration().doubleValue() / dep.getCallCount());
        }
        
        return nodeDataMap;
    }

    /**
     * 构建节点
     */
    private List<ServiceNode> buildNodes(Map<String, NodeData> nodeDataMap) {
        return nodeDataMap.entrySet().stream()
                .map(entry -> {
                    String serviceName = entry.getKey();
                    NodeData data = entry.getValue();
                    
                    double avgDuration = data.callCount > 0 ? 
                            (double) data.totalDuration / data.callCount : 0;
                    double errorRate = data.callCount > 0 ?
                            (double) data.errorCount / data.callCount * 100 : 0;
                    double p95Duration = calculateP95(data.durations);
                    int healthScore = calculateHealthScore(errorRate, avgDuration, p95Duration);
                    
                    return ServiceNode.builder()
                            .name(serviceName)
                            .callCount(data.callCount)
                            .errorCount(data.errorCount)
                            .avgDuration(avgDuration)
                            .p95Duration(p95Duration)
                            .errorRate(errorRate)
                            .healthScore(healthScore)
                            .type(determineServiceType(serviceName))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建边
     */
    private List<ServiceEdge> buildEdges(List<ServiceDependency> dependencies) {
        // 按源和目标分组聚合
        Map<String, List<ServiceDependency>> edgeGroups = dependencies.stream()
                .collect(Collectors.groupingBy(
                        dep -> dep.getFromService() + "->" + dep.getToService()
                ));
        
        return edgeGroups.values().stream()
                .map(group -> {
                    long totalCalls = group.stream()
                            .mapToLong(ServiceDependency::getCallCount)
                            .sum();
                    long totalErrors = group.stream()
                            .mapToLong(ServiceDependency::getErrorCount)
                            .sum();
                    long totalDuration = group.stream()
                            .mapToLong(ServiceDependency::getTotalDuration)
                            .sum();
                    
                    double avgDuration = totalCalls > 0 ? 
                            (double) totalDuration / totalCalls : 0;
                    double errorRate = totalCalls > 0 ?
                            (double) totalErrors / totalCalls * 100 : 0;
                    
                    ServiceDependency first = group.get(0);
                    
                    return ServiceEdge.builder()
                            .source(first.getFromService())
                            .target(first.getToService())
                            .callCount(totalCalls)
                            .errorCount(totalErrors)
                            .avgDuration(avgDuration)
                            .errorRate(errorRate)
                            .qps(calculateQPS(totalCalls, group.size()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算P95
     */
    private double calculateP95(List<Double> durations) {
        if (durations.isEmpty()) return 0;
        
        List<Double> sorted = new ArrayList<>(durations);
        Collections.sort(sorted);
        
        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.max(0, index));
    }

    /**
     * 计算健康分数
     */
    private int calculateHealthScore(double errorRate, double avgDuration, double p95Duration) {
        int score = 100;
        
        // 错误率扣分
        if (errorRate > 10) score -= 40;
        else if (errorRate > 5) score -= 25;
        else if (errorRate > 1) score -= 10;
        
        // 平均响应时间扣分
        if (avgDuration > 3000) score -= 30;
        else if (avgDuration > 1000) score -= 15;
        else if (avgDuration > 500) score -= 5;
        
        // P95扣分
        if (p95Duration > 5000) score -= 20;
        else if (p95Duration > 2000) score -= 10;
        
        return Math.max(0, score);
    }

    /**
     * 确定服务类型
     */
    private String determineServiceType(String serviceName) {
        if (serviceName.contains("external") || 
            serviceName.contains("api.") ||
            serviceName.contains("http://")) {
            return "EXTERNAL";
        }
        return "INTERNAL";
    }

    /**
     * 计算QPS
     */
    private double calculateQPS(long totalCalls, int timeBuckets) {
        if (timeBuckets == 0) return 0;
        // 假设每个时间桶是1分钟
        return (double) totalCalls / timeBuckets / 60;
    }

    /**
     * 计算统计信息
     */
    private Map<String, Object> calculateStatistics(List<ServiceNode> nodes, List<ServiceEdge> edges) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalServices", nodes.size());
        stats.put("totalDependencies", edges.size());
        
        long totalCalls = nodes.stream()
                .mapToLong(ServiceNode::getCallCount)
                .sum();
        stats.put("totalCalls", totalCalls);
        
        long totalErrors = nodes.stream()
                .mapToLong(ServiceNode::getErrorCount)
                .sum();
        stats.put("totalErrors", totalErrors);
        
        double avgErrorRate = nodes.stream()
                .mapToDouble(ServiceNode::getErrorRate)
                .average()
                .orElse(0);
        stats.put("avgErrorRate", avgErrorRate);
        
        long unhealthyServices = nodes.stream()
                .filter(n -> n.getHealthScore() < 60)
                .count();
        stats.put("unhealthyServices", unhealthyServices);
        
        return stats;
    }

    /**
     * 节点数据聚合类
     */
    private static class NodeData {
        long callCount = 0;
        long errorCount = 0;
        long totalDuration = 0;
        List<Double> durations = new ArrayList<>();
    }
}
