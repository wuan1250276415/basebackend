package com.basebackend.observability.trace.service;

import com.alibaba.fastjson2.JSON;
import com.basebackend.observability.entity.TraceSpanExt;
import com.basebackend.observability.trace.model.SpanNode;
import com.basebackend.observability.trace.model.TraceGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 追踪可视化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceVisualizationService {

    /**
     * 获取调用链可视化数据
     */
    public TraceGraph getTraceGraph(String traceId, List<TraceSpanExt> spans) {
        if (spans == null || spans.isEmpty()) {
            return TraceGraph.builder().build();
        }

        // 1. 构建树形结构
        SpanNode root = buildSpanTree(spans);
        
        // 2. 计算关键路径
        List<String> criticalPath = calculateCriticalPath(root);
        
        // 3. 标注性能瓶颈
        markBottlenecks(root, criticalPath);
        
        // 4. 计算统计信息
        Set<String> services = spans.stream()
                .map(TraceSpanExt::getServiceName)
                .collect(Collectors.toSet());
        
        return TraceGraph.builder()
                .rootSpan(root)
                .criticalPath(criticalPath)
                .totalDuration(root.getDuration())
                .spanCount(spans.size())
                .serviceCount(services.size())
                .build();
    }

    /**
     * 构建Span树
     */
    private SpanNode buildSpanTree(List<TraceSpanExt> spans) {
        Map<String, SpanNode> nodeMap = new HashMap<>();
        Map<String, List<SpanNode>> childrenMap = new HashMap<>();
        
        // 转换为节点
        for (TraceSpanExt span : spans) {
            SpanNode node = convertToNode(span);
            nodeMap.put(span.getSpanId(), node);
            
            if (span.getParentSpanId() != null) {
                childrenMap.computeIfAbsent(span.getParentSpanId(), k -> new ArrayList<>())
                        .add(node);
            }
        }
        
        // 关联子节点
        for (Map.Entry<String, List<SpanNode>> entry : childrenMap.entrySet()) {
            SpanNode parent = nodeMap.get(entry.getKey());
            if (parent != null) {
                parent.setChildren(entry.getValue());
            }
        }
        
        // 找到根节点
        return spans.stream()
                .filter(s -> s.getParentSpanId() == null)
                .findFirst()
                .map(TraceSpanExt::getSpanId)
                .map(nodeMap::get)
                .orElse(null);
    }

    /**
     * 转换为节点
     */
    private SpanNode convertToNode(TraceSpanExt span) {
        Map<String, Object> tags = span.getTags() != null ? 
                JSON.parseObject(span.getTags(), Map.class) : new HashMap<>();
        
        return SpanNode.builder()
                .spanId(span.getSpanId())
                .parentSpanId(span.getParentSpanId())
                .serviceName(span.getServiceName())
                .operationName(span.getOperationName())
                .startTime(span.getStartTime())
                .duration(span.getDuration())
                .tags(tags)
                .status(span.getStatus())
                .isError("ERROR".equals(span.getStatus()))
                .errorMessage(span.getErrorMessage())
                .children(new ArrayList<>())
                .build();
    }

    /**
     * 计算关键路径（最长耗时路径）
     */
    private List<String> calculateCriticalPath(SpanNode root) {
        List<String> path = new ArrayList<>();
        SpanNode current = root;
        
        while (current != null) {
            path.add(current.getSpanId());
            
            // 选择耗时最长的子节点
            current = current.getChildren().stream()
                    .max(Comparator.comparing(SpanNode::getDuration))
                    .orElse(null);
        }
        
        return path;
    }

    /**
     * 标注性能瓶颈
     */
    private void markBottlenecks(SpanNode root, List<String> criticalPath) {
        if (root == null) return;
        
        long threshold = (long) (root.getDuration() * 0.3); // 超过30%视为瓶颈
        
        markBottlenecksRecursive(root, threshold, criticalPath);
    }

    private void markBottlenecksRecursive(SpanNode node, long threshold, List<String> criticalPath) {
        if (node.getDuration() > threshold && criticalPath.contains(node.getSpanId())) {
            node.setIsBottleneck(true);
        }
        
        if (node.getChildren() != null) {
            for (SpanNode child : node.getChildren()) {
                markBottlenecksRecursive(child, threshold, criticalPath);
            }
        }
    }
}
