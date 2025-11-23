package com.basebackend.scheduler.workflow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * DAG 拓扑排序工具，基于 Kahn 算法实现，支持条件边过滤与并行批次识别。
 */
public final class TopologicalSorter {

    private TopologicalSorter() {
    }

    /**
     * 使用所有边执行拓扑排序。
     *
     * @param nodes 节点集合
     * @param edges 边集合
     * @return 排序结果
     */
    public static Result sort(Map<String, WorkflowNode> nodes, List<WorkflowEdge> edges) {
        return sort(nodes, edges, edge -> true);
    }

    /**
     * 支持条件过滤的拓扑排序。
     *
     * @param nodes         节点集合
     * @param edges         边集合
     * @param edgeSelector  条件边选择器，返回 true 则参与排序
     * @return 排序结果
     */
    public static Result sort(Map<String, WorkflowNode> nodes,
                              List<WorkflowEdge> edges,
                              Predicate<WorkflowEdge> edgeSelector) {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        Objects.requireNonNull(edgeSelector, "edgeSelector");

        Map<String, Integer> inDegree = initInDegree(nodes);
        Map<String, List<String>> adjacency = initAdjacency(nodes);

        for (WorkflowEdge edge : edges) {
            if (!edgeSelector.test(edge)) {
                continue;
            }
            String from = edge.getFrom();
            String to = edge.getTo();
            if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
                throw new IllegalStateException("Edge references unknown node: " + from + " -> " + to);
            }
            adjacency.get(from).add(to);
            inDegree.put(to, inDegree.get(to) + 1);
        }

        Deque<String> queue = new ArrayDeque<>(nodes.size());
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.addLast(entry.getKey());
            }
        }
        List<String> initialLayer = new ArrayList<>(queue);

        List<String> ordered = new ArrayList<>(nodes.size());
        List<List<String>> parallelLayers = new ArrayList<>();

        while (!queue.isEmpty()) {
            int batchSize = queue.size();
            List<String> batch = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize; i++) {
                String nodeId = queue.removeFirst();
                ordered.add(nodeId);
                batch.add(nodeId);
                for (String next : adjacency.get(nodeId)) {
                    int newDegree = inDegree.get(next) - 1;
                    inDegree.put(next, newDegree);
                    if (newDegree == 0) {
                        queue.addLast(next);
                    }
                }
            }
            parallelLayers.add(Collections.unmodifiableList(batch));
        }

        boolean hasCycle = ordered.size() != nodes.size();
        List<String> unresolvedNodes = hasCycle ? findUnresolvedNodes(inDegree, ordered) : Collections.emptyList();
        return new Result(ordered, parallelLayers, hasCycle, unresolvedNodes, initialLayer);
    }

    private static Map<String, Integer> initInDegree(Map<String, WorkflowNode> nodes) {
        Map<String, Integer> inDegree = new LinkedHashMap<>(Math.max(nodes.size() * 2, 16));
        for (String nodeId : nodes.keySet()) {
            inDegree.put(nodeId, 0);
        }
        return inDegree;
    }

    private static Map<String, List<String>> initAdjacency(Map<String, WorkflowNode> nodes) {
        Map<String, List<String>> adjacency = new HashMap<>(Math.max(nodes.size() * 2, 16));
        for (String nodeId : nodes.keySet()) {
            adjacency.put(nodeId, new ArrayList<>(4));
        }
        return adjacency;
    }

    private static List<String> findUnresolvedNodes(Map<String, Integer> inDegree, List<String> ordered) {
        Set<String> visited = new LinkedHashSet<>(ordered);
        List<String> unresolved = new ArrayList<>();
        inDegree.forEach((node, degree) -> {
            if (!visited.contains(node) && degree > 0) {
                unresolved.add(node);
            }
        });
        return Collections.unmodifiableList(unresolved);
    }

    /**
     * 排序结果，包含拓扑序、并行批次与循环标识。
     */
    public static final class Result {
        private final List<String> orderedNodes;
        private final List<List<String>> parallelLayers;
        private final boolean hasCycle;
        private final List<String> unresolvedNodes;
        private final List<String> initialNodes;

        Result(List<String> orderedNodes,
               List<List<String>> parallelLayers,
               boolean hasCycle,
               List<String> unresolvedNodes,
               List<String> initialNodes) {
            this.orderedNodes = Collections.unmodifiableList(new ArrayList<>(orderedNodes));
            this.parallelLayers = Collections.unmodifiableList(new ArrayList<>(parallelLayers));
            this.hasCycle = hasCycle;
            this.unresolvedNodes = unresolvedNodes;
            this.initialNodes = Collections.unmodifiableList(new ArrayList<>(initialNodes));
        }

        /**
         * 拓扑序列。
         */
        public List<String> getOrderedNodes() {
            return orderedNodes;
        }

        /**
         * 按批次分组的可并行节点列表。
         */
        public List<List<String>> getParallelLayers() {
            return parallelLayers;
        }

        /**
         * 初始入度为 0 的节点集合。
         */
        public List<String> getInitialNodes() {
            return initialNodes;
        }

        /**
         * 是否存在环。
         */
        public boolean hasCycle() {
            return hasCycle;
        }

        /**
         * 形成环的节点集合。
         */
        public List<String> getUnresolvedNodes() {
            return unresolvedNodes;
        }

        /**
         * 若存在环则返回未解决节点，否则为空列表。
         *
         * @return 未解决节点或空列表
         */
        public List<String> getReadyNodes() {
            return hasCycle ? unresolvedNodes : Collections.emptyList();
        }
    }
}
