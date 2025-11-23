package com.basebackend.scheduler.workflow;

import com.basebackend.scheduler.core.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 拓扑排序工具测试。
 */
class TopologicalSorterTest {

    @Test
    void testLinearGraph() {
        // A -> B -> C
        WorkflowNode nodeA = WorkflowNode.builder("A", "A", "processorA").build();
        WorkflowNode nodeB = WorkflowNode.builder("B", "B", "processorB").build();
        WorkflowNode nodeC = WorkflowNode.builder("C", "C", "processorC").build();
        Map<String, WorkflowNode> nodes = new HashMap<>();
        nodes.put("A", nodeA);
        nodes.put("B", nodeB);
        nodes.put("C", nodeC);

        WorkflowEdge edgeAB = WorkflowEdge.builder("A", "B").build();
        WorkflowEdge edgeBC = WorkflowEdge.builder("B", "C").build();
        List<WorkflowEdge> edges = Arrays.asList(edgeAB, edgeBC);

        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);

        assertFalse(result.hasCycle());
        assertTrue(result.getReadyNodes().isEmpty());
        assertTrue(result.getUnresolvedNodes().isEmpty());
        assertEquals(3, result.getOrderedNodes().size());
    }

    @Test
    void testDiamondGraph() {
        //    A
        //  /  \
        // B    C
        //  \  /
        //    D
        WorkflowNode nodeA = WorkflowNode.builder("A", "A", "processorA").build();
        WorkflowNode nodeB = WorkflowNode.builder("B", "B", "processorB").build();
        WorkflowNode nodeC = WorkflowNode.builder("C", "C", "processorC").build();
        WorkflowNode nodeD = WorkflowNode.builder("D", "D", "processorD").build();
        Map<String, WorkflowNode> nodes = new HashMap<>();
        nodes.put("A", nodeA);
        nodes.put("B", nodeB);
        nodes.put("C", nodeC);
        nodes.put("D", nodeD);

        WorkflowEdge edgeAB = WorkflowEdge.builder("A", "B").build();
        WorkflowEdge edgeAC = WorkflowEdge.builder("A", "C").build();
        WorkflowEdge edgeBD = WorkflowEdge.builder("B", "D").build();
        WorkflowEdge edgeCD = WorkflowEdge.builder("C", "D").build();
        List<WorkflowEdge> edges = Arrays.asList(edgeAB, edgeAC, edgeBD, edgeCD);

        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);

        assertFalse(result.hasCycle());
        assertTrue(result.getUnresolvedNodes().isEmpty());
        assertEquals(4, result.getOrderedNodes().size());
    }

    @Test
    void testCycleDetection() {
        // A -> B -> C -> A (存在环)
        WorkflowNode nodeA = WorkflowNode.builder("A", "A", "processorA").build();
        WorkflowNode nodeB = WorkflowNode.builder("B", "B", "processorB").build();
        WorkflowNode nodeC = WorkflowNode.builder("C", "C", "processorC").build();
        Map<String, WorkflowNode> nodes = new HashMap<>();
        nodes.put("A", nodeA);
        nodes.put("B", nodeB);
        nodes.put("C", nodeC);

        WorkflowEdge edgeAB = WorkflowEdge.builder("A", "B").build();
        WorkflowEdge edgeBC = WorkflowEdge.builder("B", "C").build();
        WorkflowEdge edgeCA = WorkflowEdge.builder("C", "A").build();
        List<WorkflowEdge> edges = Arrays.asList(edgeAB, edgeBC, edgeCA);

        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);

        assertTrue(result.hasCycle());
        assertFalse(result.getUnresolvedNodes().isEmpty());
    }

    @Test
    void testParallelExecution() {
        //    A
        //  / | \
        // B C D  (B、C、D可并行)
        WorkflowNode nodeA = WorkflowNode.builder("A", "A", "processorA").build();
        WorkflowNode nodeB = WorkflowNode.builder("B", "B", "processorB").build();
        WorkflowNode nodeC = WorkflowNode.builder("C", "C", "processorC").build();
        WorkflowNode nodeD = WorkflowNode.builder("D", "D", "processorD").build();
        Map<String, WorkflowNode> nodes = new HashMap<>();
        nodes.put("A", nodeA);
        nodes.put("B", nodeB);
        nodes.put("C", nodeC);
        nodes.put("D", nodeD);

        WorkflowEdge edgeAB = WorkflowEdge.builder("A", "B").build();
        WorkflowEdge edgeAC = WorkflowEdge.builder("A", "C").build();
        WorkflowEdge edgeAD = WorkflowEdge.builder("A", "D").build();
        List<WorkflowEdge> edges = Arrays.asList(edgeAB, edgeAC, edgeAD);

        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);

        assertFalse(result.hasCycle());
        List<List<String>> layers = result.getParallelLayers();
        assertEquals(2, layers.size());
        assertTrue(layers.get(0).contains("A"));
        assertTrue(layers.get(1).contains("B"));
        assertTrue(layers.get(1).contains("C"));
        assertTrue(layers.get(1).contains("D"));
    }

    @Test
    void testEmptyGraph() {
        TopologicalSorter.Result result = TopologicalSorter.sort(new HashMap<>(), List.of());
        assertFalse(result.hasCycle());
        assertTrue(result.getReadyNodes().isEmpty());
        assertTrue(result.getOrderedNodes().isEmpty());
    }
}
