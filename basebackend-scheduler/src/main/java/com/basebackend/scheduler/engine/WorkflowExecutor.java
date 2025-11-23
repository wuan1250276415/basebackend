package com.basebackend.scheduler.engine;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskResult;
import com.basebackend.scheduler.core.RetryTemplate;
import com.basebackend.scheduler.metrics.MetricsCollector;
import com.basebackend.scheduler.processor.ProcessorRegistry;
import com.basebackend.scheduler.core.IdempotentCache;
import com.basebackend.scheduler.workflow.WorkflowDefinition;
import com.basebackend.scheduler.workflow.WorkflowInstance;
import com.basebackend.scheduler.workflow.WorkflowNode;
import com.basebackend.scheduler.workflow.WorkflowEdge;
import com.basebackend.scheduler.workflow.WorkflowExecutionLog;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DAG 执行核心：负责节点调度、并行推进、终态判定与指标采集。
 */
@Component
@ConditionalOnProperty(prefix = "scheduler.workflow.executor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final ProcessorRegistry processorRegistry;
    private final RetryTemplate retryTemplate;
    private final MetricsCollector metricsCollector;
    private final IdempotentCache<WorkflowExecutionLog> idempotentCache;
    private final ExecutorService executorService;
    private final Duration defaultTimeout;

    public WorkflowExecutor(ProcessorRegistry processorRegistry,
                            RetryTemplate retryTemplate,
                            MetricsCollector metricsCollector,
                            IdempotentCache<WorkflowExecutionLog> idempotentCache) {
        this(processorRegistry, retryTemplate, metricsCollector, idempotentCache, Executors.newCachedThreadPool(), Duration.ofMinutes(5));
    }

    public WorkflowExecutor(ProcessorRegistry processorRegistry,
                            RetryTemplate retryTemplate,
                            MetricsCollector metricsCollector,
                            IdempotentCache<WorkflowExecutionLog> idempotentCache,
                            ExecutorService executorService,
                            Duration defaultTimeout) {
        this.processorRegistry = processorRegistry;
        this.retryTemplate = retryTemplate;
        this.metricsCollector = metricsCollector;
        this.idempotentCache = idempotentCache;
        this.executorService = executorService;
        this.defaultTimeout = defaultTimeout == null ? Duration.ofMinutes(5) : defaultTimeout;
    }

    /**
     * 并行执行工作流。遇到节点失败将提前终止。
     */
    public WorkflowExecutionLog execute(WorkflowDefinition definition, WorkflowInstance instance) {
        Map<String, WorkflowNode> nodes = definition.getNodes();
        Map<String, List<String>> graph = buildGraph(definition.getEdges());
        Map<String, Integer> inDegree = buildInDegree(nodes, definition.getEdges());

        Queue<WorkflowNode> ready = new ArrayDeque<>(nodes.size());
        inDegree.forEach((id, deg) -> {
            if (deg == 0) {
                ready.add(nodes.get(id));
            }
        });

        Map<String, WorkflowExecutionLog> nodeLogs = new HashMap<>(nodes.size());
        boolean failed = false;

        while (!ready.isEmpty() && !failed) {
            List<WorkflowNode> batch = drainReady(ready);
            List<Future<WorkflowExecutionLog>> futures = new ArrayList<>();
            for (WorkflowNode node : batch) {
                futures.add(executorService.submit(() -> executeNode(instance, node)));
            }
            for (int i = 0; i < batch.size(); i++) {
                WorkflowNode node = batch.get(i);
                try {
                    WorkflowExecutionLog logEntry = futures.get(i).get(defaultTimeout.toMillis(), TimeUnit.MILLISECONDS);
                    nodeLogs.put(node.getId(), logEntry);
                    if (logEntry.getStatus() != WorkflowExecutionLog.Status.SUCCESS) {
                        failed = true;
                        cancelRemaining(futures, i + 1);
                        break;
                    }
                    propagateSuccess(node, graph, inDegree, nodes, ready);
                } catch (TimeoutException timeout) {
                    failed = true;
                    cancelRemaining(futures, i);
                    WorkflowExecutionLog logEntry = WorkflowExecutionLog.builder(node.getId(), instance.getId(), WorkflowExecutionLog.Status.FAILED)
                            .errorMessage("Timeout after " + defaultTimeout)
                            .build();
                    nodeLogs.put(node.getId(), logEntry);
                    metricsCollector.recordResult(node.getProcessorType(), TaskResult.builder(TaskResult.Status.CANCELLED)
                            .errorMessage("Timeout: " + timeout.getMessage())
                            .build());
                    log.error("Workflow node [{}] timed out", node.getId(), timeout);
                } catch (Exception ex) {
                    failed = true;
                    cancelRemaining(futures, i);
                    WorkflowExecutionLog logEntry = WorkflowExecutionLog.builder(node.getId(), instance.getId(), WorkflowExecutionLog.Status.FAILED)
                            .errorMessage(ex.getMessage())
                            .build();
                    nodeLogs.put(node.getId(), logEntry);
                    metricsCollector.recordResult(node.getProcessorType(), TaskResult.builder(TaskResult.Status.FAILED)
                            .errorMessage(ex.getMessage())
                            .build());
                    log.error("Workflow node [{}] failed", node.getId(), ex);
                }
            }
        }

        boolean allDone = nodeLogs.size() == nodes.size() && !failed;
        if (allDone) {
            return WorkflowExecutionLog.builder("workflow", instance.getId(), WorkflowExecutionLog.Status.SUCCESS)
                    .build();
        }
        return WorkflowExecutionLog.builder("workflow", instance.getId(), WorkflowExecutionLog.Status.FAILED)
                .build();
    }

    private WorkflowExecutionLog executeNode(WorkflowInstance instance, WorkflowNode node) {
        String cacheKey = instance.getId() + ":" + node.getId();
        Optional<WorkflowExecutionLog> cachedOpt = idempotentCache.get(cacheKey);
        if (cachedOpt.isPresent()) {
            log.debug("Skip node [{}] due to idempotent cache hit", node.getId());
            return cachedOpt.get();
        }
        Instant start = Instant.now();
        try {
            TaskProcessor processor = locateProcessor(node);
            TaskContext context = TaskContext.builder("workflow-task")
                    .build();
            TaskResult result = processor.process(context);
            metricsCollector.recordLatency(node.getProcessorType(), Duration.between(start, Instant.now()));
            metricsCollector.recordResult(node.getProcessorType(), result);

            WorkflowExecutionLog logEntry = WorkflowExecutionLog.builder(node.getId(), instance.getId(),
                    result.isSuccess() ? WorkflowExecutionLog.Status.SUCCESS : WorkflowExecutionLog.Status.FAILED)
                    .errorMessage(result.getErrorMessage())
                    .build();

            if (result.isSuccess()) {
                idempotentCache.put(cacheKey, logEntry);
            }
            return logEntry;
        } catch (Exception ex) {
            metricsCollector.recordResult(node.getProcessorType(), TaskResult.builder(TaskResult.Status.FAILED)
                    .errorMessage(ex.getMessage())
                    .build());
            return WorkflowExecutionLog.builder(node.getId(), instance.getId(), WorkflowExecutionLog.Status.FAILED)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    private TaskProcessor locateProcessor(WorkflowNode node) {
        Optional<TaskProcessor> found = processorRegistry.find(node.getProcessorType());
        if (!found.isPresent()) {
            throw new IllegalStateException("Processor not found: " + node.getProcessorType());
        }
        return found.get();
    }

    private Map<String, List<String>> buildGraph(List<WorkflowEdge> edges) {
        Map<String, List<String>> graph = new HashMap<>(Math.max(edges.size(), 16));
        for (WorkflowEdge edge : edges) {
            graph.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge.getTo());
        }
        return graph;
    }

    private Map<String, Integer> buildInDegree(Map<String, WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Map<String, Integer> inDegree = new HashMap<>(Math.max(nodes.size() * 2, 16));
        nodes.keySet().forEach(id -> inDegree.put(id, 0));
        for (WorkflowEdge edge : edges) {
            inDegree.computeIfPresent(edge.getTo(), (k, v) -> v + 1);
        }
        return inDegree;
    }

    private void cancelRemaining(List<Future<WorkflowExecutionLog>> futures, int startIndex) {
        for (int i = startIndex; i < futures.size(); i++) {
            futures.get(i).cancel(true);
        }
    }

    private void propagateSuccess(WorkflowNode current,
                                  Map<String, List<String>> graph,
                                  Map<String, Integer> inDegree,
                                  Map<String, WorkflowNode> nodes,
                                  Queue<WorkflowNode> ready) {
        List<String> downstream = graph.getOrDefault(current.getId(), Collections.emptyList());
        for (String target : downstream) {
            int left = inDegree.computeIfPresent(target, (k, v) -> v - 1);
            if (left == 0) {
                ready.add(nodes.get(target));
            }
        }
    }

    private List<WorkflowNode> drainReady(Queue<WorkflowNode> ready) {
        List<WorkflowNode> batch = new ArrayList<>();
        WorkflowNode node;
        while ((node = ready.poll()) != null) {
            batch.add(node);
        }
        return batch;
    }

    /**
     * 优雅关闭执行器。
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
        log.info("WorkflowExecutor shutdown");
    }
}
