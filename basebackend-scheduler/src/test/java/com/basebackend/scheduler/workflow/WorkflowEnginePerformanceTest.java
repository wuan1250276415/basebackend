package com.basebackend.scheduler.workflow;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流引擎性能基准测试。
 *
 * <p>测试包括：
 * - 拓扑排序性能
 * - 并发工作流执行
 * - 大规模DAG处理
 */
@Slf4j
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.redis.host=localhost"
})
class WorkflowEnginePerformanceTest {

    /**
     * 测试大规模拓扑排序性能。
     * 1000个节点，5000条边的DAG排序。
     */
    @Test
    void testLargeScaleTopologicalSort() {
        int nodeCount = 1000;
        int edgeCount = 5000;

        // 创建大规模节点
        Map<String, WorkflowNode> nodes = new HashMap<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.put("node-" + i, createMockNode("node-" + i));
        }

        // 创建大规模边（确保无环）
        List<WorkflowEdge> edges = new ArrayList<>();
        Random random = new Random(42); // 固定种子保证可重复性
        for (int i = 0; i < edgeCount; i++) {
            int from = random.nextInt(nodeCount - 100);
            int to = from + 50 + random.nextInt(50);
            edges.add(WorkflowEdge.builder("node-" + from, "node-" + to).build());
        }

        log.info("开始大规模拓扑排序测试：{}个节点，{}条边", nodeCount, edgeCount);

        long startTime = System.nanoTime();
        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("拓扑排序完成：耗时 {:.2f}ms，排序节点数：{}", durationMs, result.getOrderedNodes().size());
        log.info("并行层数：{}，初始节点数：{}", result.getParallelLayers().size(), result.getInitialNodes().size());

        // 验证结果
        assertEquals(nodeCount, result.getOrderedNodes().size());
        assertFalse(result.hasCycle());
        assertFalse(result.getInitialNodes().isEmpty());

        // 性能断言：1000个节点应该在100ms内完成
        assertTrue(durationMs < 100,
            "大规模拓扑排序耗时过长：%.2fms，期望小于100ms".formatted(durationMs));

        log.info("✅ 大规模拓扑排序性能测试通过");
    }

    /**
     * 测试深度链路拓扑排序性能。
     * 10000个节点的单链路。
     */
    @Test
    void testDeepChainTopologicalSort() {
        int nodeCount = 10000;

        // 创建单链路节点
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.put("chain-" + i, createMockNode("chain-" + i));
        }

        // 创建单链路边
        List<WorkflowEdge> edges = new ArrayList<>();
        for (int i = 0; i < nodeCount - 1; i++) {
            edges.add(WorkflowEdge.builder("chain-" + i, "chain-" + (i + 1)).build());
        }

        log.info("开始深度链路拓扑排序测试：{}个节点", nodeCount);

        long startTime = System.nanoTime();
        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("深度链路排序完成：耗时 {:.2fms}，层数：{}",
            durationMs, result.getParallelLayers().size());

        // 验证结果
        assertEquals(nodeCount, result.getOrderedNodes().size());
        assertFalse(result.hasCycle());
        assertEquals(1, result.getInitialNodes().size());

        // 性能断言：单链路排序应该在50ms内完成
        assertTrue(durationMs < 50,
            "深度链路排序耗时过长：%.2fms，期望小于50ms".formatted(durationMs));

        log.info("✅ 深度链路拓扑排序性能测试通过");
    }

    /**
     * 测试并发拓扑排序性能。
     * 100个并发线程同时进行排序。
     */
    @Test
    void testConcurrentTopologicalSort() throws InterruptedException {
        int threadCount = 100;
        int nodeCount = 100;

        // 创建标准DAG
        Map<String, WorkflowNode> nodes = createStandardDAG(nodeCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        long[] totalTime = new long[1];
        long[] successCount = new long[1];
        long[] errorCount = new long[1];

        log.info("开始并发拓扑排序测试：{}个线程，{}个节点", threadCount, nodeCount);

        long testStartTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    TopologicalSorter.Result result = TopologicalSorter.sort(nodes, createStandardEdges(nodeCount));
                    long endTime = System.nanoTime();

                    synchronized (totalTime) {
                        totalTime[0] += (endTime - startTime);
                        successCount[0]++;
                    }

                    // 验证结果正确性
                    assertEquals(nodeCount, result.getOrderedNodes().size());
                    assertFalse(result.hasCycle());
                } catch (Exception e) {
                    synchronized (errorCount) {
                        errorCount[0]++;
                    }
                    log.error("线程-{}执行失败", threadId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long testEndTime = System.nanoTime();
        double totalDurationMs = (testEndTime - testStartTime) / 1_000_000.0;
        double avgDurationMs = totalTime[0] / successCount[0] / 1_000_000.0;

        log.info("并发测试完成：");
        log.info("  总耗时：{:.2f}ms", totalDurationMs);
        log.info("  平均耗时：{:.2f}ms", avgDurationMs);
        log.info("  成功：{}，失败：{}", successCount[0], errorCount[0]);

        // 验证结果
        assertEquals(threadCount, successCount[0], "所有线程都应该成功执行");
        assertEquals(0, errorCount[0], "不应该有失败的线程");

        // 性能断言：并发执行应该稳定，不出现明显竞争
        assertTrue(avgDurationMs < 10,
            "平均排序耗时过长：%.2fms".formatted(avgDurationMs));

        log.info("✅ 并发拓扑排序性能测试通过");
    }

    /**
     * 测试宽分支拓扑排序性能。
     * 100个并发分支。
     */
    @Test
    void testWideBranchTopologicalSort() {
        int branchCount = 100;

        // 创建宽分支DAG
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        nodes.put("start", createMockNode("start"));

        // 创建100个并行分支
        for (int i = 0; i < branchCount; i++) {
            String branchNode = "branch-" + i;
            nodes.put(branchNode, createMockNode(branchNode));
            nodes.put(branchNode + "-end", createMockNode(branchNode + "-end"));
        }

        // 创建边
        List<WorkflowEdge> edges = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            String branchNode = "branch-" + i;
            // start -> branch-i
            edges.add(WorkflowEdge.builder("start", branchNode).build());
            // branch-i -> branch-i-end
            edges.add(WorkflowEdge.builder(branchNode, branchNode + "-end").build());
        }

        log.info("开始宽分支拓扑排序测试：{}个并行分支", branchCount);

        long startTime = System.nanoTime();
        TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("宽分支排序完成：耗时 {:.2f}ms，并行层数：{}",
            durationMs, result.getParallelLayers().size());

        // 验证结果
        assertEquals(branchCount * 2 + 1, result.getOrderedNodes().size());
        assertFalse(result.hasCycle());

        // 第一层应该是start节点，第二层应该是所有branch节点
        assertEquals(1, result.getParallelLayers().get(0).size());
        assertEquals("start", result.getParallelLayers().get(0).get(0));

        log.info("✅ 宽分支拓扑排序性能测试通过");
    }

    /**
     * 测试内存使用情况。
     */
    @Test
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        int iterations = 10;

        // 强制GC并记录初始内存
        System.gc();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();

        log.info("初始内存使用：{} MB", usedMemoryBefore / 1024 / 1024);

        long[] peakMemory = new long[1];

        for (int i = 0; i < iterations; i++) {
            // 创建大规模DAG
            Map<String, WorkflowNode> nodes = createStandardDAG(500);
            List<WorkflowEdge> edges = createStandardEdges(500);

            // 执行拓扑排序
            TopologicalSorter.Result result = TopologicalSorter.sort(nodes, edges);

            // 检查内存使用
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            peakMemory[0] = Math.max(peakMemory[0], usedMemory);

            // 验证结果
            assertEquals(500, result.getOrderedNodes().size());
        }

        // 清理引用
        System.gc();

        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long peakMemoryDelta = peakMemory[0] - usedMemoryBefore;

        log.info("峰值内存使用：{} MB", peakMemoryDelta / 1024 / 1024);
        log.info("当前内存使用：{} MB", usedMemoryAfter / 1024 / 1024);

        // 性能断言：500个节点的DAG不应该消耗超过100MB内存
        assertTrue(peakMemoryDelta < 100 * 1024 * 1024,
            "内存使用过多：%d MB，期望小于100 MB".formatted(peakMemoryDelta / 1024 / 1024));

        log.info("✅ 内存使用测试通过");
    }

    // ===== 辅助方法 =====

    private WorkflowNode createMockNode(String id) {
        return WorkflowNode.builder(id, "Mock Node", "mock-processor")
                .build();
    }

    private Map<String, WorkflowNode> createStandardDAG(int nodeCount) {
        Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.put("node-" + i, createMockNode("node-" + i));
        }
        return nodes;
    }

    private List<WorkflowEdge> createStandardEdges(int nodeCount) {
        List<WorkflowEdge> edges = new ArrayList<>();
        for (int i = 0; i < nodeCount - 1; i++) {
            edges.add(WorkflowEdge.builder("node-" + i, "node-" + (i + 1)).build());
        }
        // 添加一些横向边增加复杂性
        for (int i = 0; i < nodeCount / 10; i++) {
            int from = i * 10;
            int to = from + 5;
            if (to < nodeCount) {
                edges.add(WorkflowEdge.builder("node-" + from, "node-" + to).build());
            }
        }
        return edges;
    }
}
