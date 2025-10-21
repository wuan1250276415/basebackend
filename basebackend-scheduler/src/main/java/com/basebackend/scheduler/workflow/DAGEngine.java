package com.basebackend.scheduler.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DAG工作流引擎
 * 支持拓扑排序、并行执行、条件分支
 */
@Slf4j
@Component
public class DAGEngine {

    /**
     * 执行工作流
     *
     * @param workflow 工作流定义
     * @return 执行结果
     */
    public Map<String, Object> execute(WorkflowDefinition workflow) {
        log.info("开始执行工作流: {}", workflow.getName());

        // 1. 构建依赖图
        Map<String, List<String>> dependencyGraph = buildDependencyGraph(workflow);

        // 2. 拓扑排序
        List<Set<String>> executionLevels = topologicalSort(workflow, dependencyGraph);

        // 3. 按层级执行
        Map<String, Object> results = new ConcurrentHashMap<>();
        for (Set<String> level : executionLevels) {
            executeLevel(workflow, level, results);
        }

        log.info("工作流执行完成: {}", workflow.getName());
        return results;
    }

    /**
     * 构建依赖图
     */
    private Map<String, List<String>> buildDependencyGraph(WorkflowDefinition workflow) {
        Map<String, List<String>> graph = new HashMap<>();

        // 初始化所有节点
        for (WorkflowNode node : workflow.getNodes()) {
            graph.putIfAbsent(node.getNodeId(), new ArrayList<>());
        }

        // 构建依赖关系
        for (WorkflowEdge edge : workflow.getEdges()) {
            graph.get(edge.getToNodeId()).add(edge.getFromNodeId());
        }

        return graph;
    }

    /**
     * 拓扑排序 - 将任务分层
     */
    private List<Set<String>> topologicalSort(WorkflowDefinition workflow,
                                               Map<String, List<String>> dependencyGraph) {
        List<Set<String>> levels = new ArrayList<>();
        Set<String> completed = new HashSet<>();

        while (completed.size() < workflow.getNodes().size()) {
            Set<String> currentLevel = new HashSet<>();

            for (WorkflowNode node : workflow.getNodes()) {
                String nodeId = node.getNodeId();

                // 跳过已完成的节点
                if (completed.contains(nodeId)) {
                    continue;
                }

                // 检查依赖是否全部完成
                List<String> dependencies = dependencyGraph.get(nodeId);
                if (dependencies == null || dependencies.isEmpty() ||
                        completed.containsAll(dependencies)) {
                    currentLevel.add(nodeId);
                }
            }

            if (currentLevel.isEmpty()) {
                throw new IllegalStateException("检测到循环依赖或无法解析的依赖关系");
            }

            levels.add(currentLevel);
            completed.addAll(currentLevel);
        }

        log.debug("工作流拓扑排序完成，共{}层", levels.size());
        return levels;
    }

    /**
     * 执行一个层级（并行执行）
     */
    private void executeLevel(WorkflowDefinition workflow, Set<String> nodeIds,
                              Map<String, Object> results) {
        List<CompletableFuture<Void>> futures = nodeIds.stream()
                .map(nodeId -> CompletableFuture.runAsync(() -> {
                    WorkflowNode node = findNode(workflow, nodeId);
                    if (node != null) {
                        executeNode(node, results);
                    }
                }))
                .collect(Collectors.toList());

        // 等待当前层级所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 执行单个节点
     */
    private void executeNode(WorkflowNode node, Map<String, Object> results) {
        try {
            log.info("执行节点: {}", node.getNodeName());

            // TODO: 根据processorType实例化处理器并执行
            // 这里简化处理，实际应该通过Spring容器获取Bean或反射创建实例

            Object result = Map.of(
                    "nodeId", node.getNodeId(),
                    "status", "SUCCESS",
                    "message", "Node executed successfully"
            );

            results.put(node.getNodeId(), result);
            log.info("节点执行完成: {}", node.getNodeName());

        } catch (Exception e) {
            log.error("节点执行失败: {}", node.getNodeName(), e);
            if (!node.getAllowFailure()) {
                throw new RuntimeException("节点执行失败: " + node.getNodeName(), e);
            }
            results.put(node.getNodeId(), Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }

    /**
     * 查找节点
     */
    private WorkflowNode findNode(WorkflowDefinition workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
}
