package com.basebackend.observability.trace.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.basebackend.observability.trace.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 性能瓶颈检测服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceBottleneckDetector {

    /**
     * 检测所有性能瓶颈
     */
    public List<Bottleneck> detectBottlenecks(TraceGraph graph) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        if (graph == null || graph.getRootSpan() == null) {
            return bottlenecks;
        }

        // 规则1: 单个Span耗时超过总时长的30%
        long threshold = (long) (graph.getTotalDuration() * 0.3);
        findSlowSpans(graph.getRootSpan(), threshold, bottlenecks);
        
        // 规则2: N+1查询问题
        detectNPlusOneQuery(graph, bottlenecks);
        
        // 规则3: 串行调用多次相同服务
        detectSerialCalls(graph, bottlenecks);
        
        // 规则4: 外部服务调用超时
        detectExternalServiceIssues(graph, bottlenecks);
        
        return bottlenecks;
    }

    /**
     * 查找慢Span
     */
    private void findSlowSpans(SpanNode node, long threshold, List<Bottleneck> bottlenecks) {
        if (node == null) return;
        
        if (node.getDuration() > threshold) {
            bottlenecks.add(Bottleneck.builder()
                    .type(BottleneckType.SLOW_SPAN)
                    .severity(determineSeverity(node.getDuration(), threshold))
                    .description(String.format("%s 耗时 %dms，占比 %.1f%%", 
                            node.getOperationName(),
                            node.getDuration(),
                            (double) node.getDuration() / threshold * 30))
                    .spanIds(Collections.singletonList(node.getSpanId()))
                    .totalDuration(node.getDuration())
                    .location(node.getServiceName() + "." + node.getOperationName())
                    .suggestion("优化该操作的执行效率，考虑使用缓存或异步处理")
                    .build());
        }
        
        if (node.getChildren() != null) {
            for (SpanNode child : node.getChildren()) {
                findSlowSpans(child, threshold, bottlenecks);
            }
        }
    }

    /**
     * 检测N+1查询问题
     */
    private void detectNPlusOneQuery(TraceGraph graph, List<Bottleneck> bottlenecks) {
        Map<String, List<SpanNode>> dbQueryGroups = new HashMap<>();
        
        // 收集所有数据库查询Span
        collectDbSpans(graph.getRootSpan(), dbQueryGroups);
        
        for (Map.Entry<String, List<SpanNode>> entry : dbQueryGroups.entrySet()) {
            List<SpanNode> spans = entry.getValue();
            
            // 如果相同的查询被执行超过10次，可能是N+1问题
            if (spans.size() > 10) {
                long totalDuration = spans.stream()
                        .mapToLong(SpanNode::getDuration)
                        .sum();
                
                bottlenecks.add(Bottleneck.builder()
                        .type(BottleneckType.N_PLUS_ONE_QUERY)
                        .severity(Severity.HIGH)
                        .description(String.format("检测到N+1查询问题：相同查询执行了 %d 次", spans.size()))
                        .spanIds(spans.stream()
                                .map(SpanNode::getSpanId)
                                .collect(Collectors.toList()))
                        .sqlQuery(entry.getKey())
                        .queryCount(spans.size())
                        .totalDuration(totalDuration)
                        .location(spans.get(0).getServiceName())
                        .suggestion("使用JOIN或批量查询替代循环查询，或使用DataLoader模式")
                        .build());
            }
        }
    }

    /**
     * 收集数据库Span
     */
    private void collectDbSpans(SpanNode node, Map<String, List<SpanNode>> groups) {
        if (node == null) return;
        
        // 检查是否是数据库操作
        if (isDbOperation(node)) {
            String queryKey = extractQueryKey(node);
            groups.computeIfAbsent(queryKey, k -> new ArrayList<>()).add(node);
        }
        
        if (node.getChildren() != null) {
            for (SpanNode child : node.getChildren()) {
                collectDbSpans(child, groups);
            }
        }
    }

    /**
     * 判断是否是数据库操作
     */
    private boolean isDbOperation(SpanNode node) {
        if (node.getTags() == null) return false;
        
        Object component = node.getTags().get("component");
        if (component != null) {
            String comp = component.toString().toLowerCase();
            return comp.contains("mysql") || 
                   comp.contains("postgresql") || 
                   comp.contains("jdbc") ||
                   comp.contains("mybatis");
        }
        
        return node.getOperationName().toLowerCase().contains("select") ||
               node.getOperationName().toLowerCase().contains("query");
    }

    /**
     * 提取查询关键字（用于分组）
     */
    private String extractQueryKey(SpanNode node) {
        if (node.getTags() != null && node.getTags().containsKey("db.statement")) {
            String sql = node.getTags().get("db.statement").toString();
            // 去除参数，只保留SQL结构
            return normalizeSql(sql);
        }
        return node.getOperationName();
    }

    /**
     * 规范化SQL（去除参数值）
     */
    private String normalizeSql(String sql) {
        // 简单实现：将数字和字符串替换为占位符
        return sql.replaceAll("'[^']*'", "?")
                  .replaceAll("\\d+", "?")
                  .replaceAll("\\s+", " ")
                  .trim();
    }

    /**
     * 检测串行调用
     */
    private void detectSerialCalls(TraceGraph graph, List<Bottleneck> bottlenecks) {
        Map<String, List<SpanNode>> serviceCallGroups = new HashMap<>();
        
        // 收集对同一服务的调用
        collectServiceCalls(graph.getRootSpan(), serviceCallGroups);
        
        for (Map.Entry<String, List<SpanNode>> entry : serviceCallGroups.entrySet()) {
            List<SpanNode> spans = entry.getValue();
            
            // 如果串行调用同一服务超过5次
            if (spans.size() > 5 && areSequential(spans)) {
                long totalDuration = spans.stream()
                        .mapToLong(SpanNode::getDuration)
                        .sum();
                
                bottlenecks.add(Bottleneck.builder()
                        .type(BottleneckType.SERIAL_CALLS)
                        .severity(Severity.MEDIUM)
                        .description(String.format("串行调用 %s 服务 %d 次", entry.getKey(), spans.size()))
                        .spanIds(spans.stream()
                                .map(SpanNode::getSpanId)
                                .collect(Collectors.toList()))
                        .queryCount(spans.size())
                        .totalDuration(totalDuration)
                        .location(entry.getKey())
                        .suggestion("考虑使用批量接口或并行调用来优化性能")
                        .build());
            }
        }
    }

    /**
     * 收集服务调用
     */
    private void collectServiceCalls(SpanNode node, Map<String, List<SpanNode>> groups) {
        if (node == null || node.getChildren() == null) return;
        
        for (SpanNode child : node.getChildren()) {
            if (isServiceCall(child)) {
                String serviceKey = child.getServiceName();
                groups.computeIfAbsent(serviceKey, k -> new ArrayList<>()).add(child);
            }
            collectServiceCalls(child, groups);
        }
    }

    /**
     * 判断是否是服务调用
     */
    private boolean isServiceCall(SpanNode node) {
        if (node.getTags() == null) return false;
        
        Object spanKind = node.getTags().get("span.kind");
        return spanKind != null && "client".equals(spanKind.toString());
    }

    /**
     * 判断调用是否是串行的
     */
    private boolean areSequential(List<SpanNode> spans) {
        if (spans.size() < 2) return false;
        
        // 按开始时间排序
        List<SpanNode> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparing(SpanNode::getStartTime));
        
        // 检查是否有时间重叠
        for (int i = 0; i < sorted.size() - 1; i++) {
            SpanNode current = sorted.get(i);
            SpanNode next = sorted.get(i + 1);
            
            long currentEnd = current.getStartTime() + current.getDuration();
            if (next.getStartTime() < currentEnd) {
                // 有重叠，不是完全串行
                return false;
            }
        }
        
        return true;
    }

    /**
     * 检测外部服务问题
     */
    private void detectExternalServiceIssues(TraceGraph graph, List<Bottleneck> bottlenecks) {
        detectExternalTimeouts(graph.getRootSpan(), bottlenecks);
    }

    private void detectExternalTimeouts(SpanNode node, List<Bottleneck> bottlenecks) {
        if (node == null) return;
        
        // 检查HTTP调用超时
        if (isHttpCall(node) && node.getDuration() > 3000) { // 超过3秒
            bottlenecks.add(Bottleneck.builder()
                    .type(BottleneckType.EXTERNAL_SERVICE_TIMEOUT)
                    .severity(Severity.HIGH)
                    .description(String.format("外部服务调用超时：%s 耗时 %dms", 
                            node.getOperationName(), node.getDuration()))
                    .spanIds(Collections.singletonList(node.getSpanId()))
                    .totalDuration(node.getDuration())
                    .location(node.getServiceName() + " -> " + extractTargetService(node))
                    .suggestion("检查网络连接，增加超时配置，或使用降级策略")
                    .build());
        }
        
        if (node.getChildren() != null) {
            for (SpanNode child : node.getChildren()) {
                detectExternalTimeouts(child, bottlenecks);
            }
        }
    }

    private boolean isHttpCall(SpanNode node) {
        if (node.getTags() == null) return false;
        
        return node.getTags().containsKey("http.url") ||
               node.getTags().containsKey("http.method");
    }

    private String extractTargetService(SpanNode node) {
        if (node.getTags() != null && node.getTags().containsKey("http.url")) {
            String url = node.getTags().get("http.url").toString();
            // 简单提取域名
            try {
                return new java.net.URL(url).getHost();
            } catch (Exception e) {
                return "unknown";
            }
        }
        return "unknown";
    }

    /**
     * 确定严重程度
     */
    private Severity determineSeverity(long duration, long threshold) {
        double ratio = (double) duration / threshold;
        
        if (ratio > 2.0) return Severity.CRITICAL;
        if (ratio > 1.5) return Severity.HIGH;
        if (ratio > 1.0) return Severity.MEDIUM;
        return Severity.LOW;
    }
}
