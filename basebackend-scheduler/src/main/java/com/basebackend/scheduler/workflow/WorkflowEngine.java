package com.basebackend.scheduler.workflow;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskResult;
import com.basebackend.scheduler.processor.ProcessorRegistry;
import com.basebackend.scheduler.engine.WorkflowExecutor;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 工作流引擎入口，负责接收定义与实例并委托 WorkflowExecutor 运行。
 */
@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final Map<String, WorkflowDefinition> definitions;
    private final Map<String, WorkflowInstance> instances;
    private final CopyOnWriteArrayList<WorkflowEventListener> listeners;
    private final WorkflowExecutor workflowExecutor;
    private final ProcessorRegistry processorRegistry;

    public WorkflowEngine(WorkflowExecutor workflowExecutor, ProcessorRegistry processorRegistry) {
        this.definitions = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.workflowExecutor = workflowExecutor;
        this.processorRegistry = processorRegistry;
    }

    /**
     * 创建并注册工作流定义。
     *
     * @param definition 工作流定义
     * @return 注册后的定义
     */
    public WorkflowDefinition createWorkflow(WorkflowDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        TopologicalSorter.Result topology = validateWorkflow(definition);
        if (topology.hasCycle()) {
            throw new IllegalStateException("Workflow definition contains cycle. Unresolved nodes: " + topology.getUnresolvedNodes());
        }
        WorkflowDefinition previous = definitions.putIfAbsent(definition.getId(), definition);
        if (previous != null) {
            throw new IllegalStateException("Workflow definition already exists: " + definition.getId());
        }
        return definition;
    }

    /**
     * 校验工作流拓扑。
     *
     * @param definition 工作流定义
     * @return 拓扑排序结果
     */
    public TopologicalSorter.Result validateWorkflow(WorkflowDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return definition.validateTopology();
    }

    /**
     * 启动实例，自动生成实例 ID。
     *
     * @param definitionId 工作流定义 ID
     * @param params       上下文参数
     * @return 工作流实例
     */
    public WorkflowInstance startWorkflow(String definitionId, Map<String, Object> params) {
        String instanceId = UUID.randomUUID().toString();
        return startWorkflow(instanceId, definitionId, params);
    }

    /**
     * 启动实例，使用指定实例 ID。
     *
     * @param instanceId   实例 ID
     * @param definitionId 工作流定义 ID
     * @param params       上下文参数
     * @return 工作流实例
     */
    public WorkflowInstance startWorkflow(String instanceId, String definitionId, Map<String, Object> params) {
        WorkflowDefinition definition = requireDefinition(definitionId);
        TopologicalSorter.Result topology = definition.validateTopology();
        if (topology.hasCycle()) {
            throw new IllegalStateException("Workflow instance cannot start: definition contains cycle. Unresolved nodes: " + topology.getUnresolvedNodes());
        }
        Set<String> readyNodes = Set.copyOf(topology.getInitialNodes());

        WorkflowInstance instance = WorkflowInstance.builder(instanceId, definitionId)
                .status(WorkflowInstance.Status.RUNNING)
                .context(params != null ? params : Collections.emptyMap())
                .activeNodes(readyNodes)
                .startTime(Instant.now())
                .build();
        instances.put(instanceId, instance);
        readyNodes.forEach(nodeId -> notifyNodeStart(instanceId, nodeId));
        return instance;
    }

    /**
     * 执行工作流。
     */
    public WorkflowExecutionLog run(WorkflowDefinition definition, WorkflowInstance instance) {
        log.info("Start workflow instance [{}] with definition [{}]", instance.getId(), definition.getName());
        return workflowExecutor.execute(definition, instance);
    }

    /**
     * 暂停实例。
     *
     * @param instanceId 实例 ID
     * @return 更新后的实例
     */
    public WorkflowInstance pauseWorkflow(String instanceId) {
        return updateInstance(instanceId, current -> {
            ensureStatus(current, WorkflowInstance.Status.RUNNING);
            return current.toBuilder()
                    .status(WorkflowInstance.Status.PAUSED)
                    .build();
        });
    }

    /**
     * 恢复实例。
     *
     * @param instanceId 实例 ID
     * @return 更新后的实例
     */
    public WorkflowInstance resumeWorkflow(String instanceId) {
        return updateInstance(instanceId, current -> {
            ensureStatus(current, WorkflowInstance.Status.PAUSED);
            return current.toBuilder()
                    .status(WorkflowInstance.Status.RUNNING)
                    .build();
        });
    }

    /**
     * 取消实例。
     *
     * @param instanceId 实例 ID
     * @return 更新后的实例
     */
    public WorkflowInstance cancelWorkflow(String instanceId) {
        return updateInstance(instanceId, current -> {
            if (current.isTerminal()) {
                return current;
            }
            return current.toBuilder()
                    .status(WorkflowInstance.Status.CANCELLED)
                    .endTime(Instant.now())
                    .build();
        });
    }

    /**
     * 查询实例状态。
     *
     * @param instanceId 实例 ID
     * @return 实例信息
     */
    public WorkflowInstance getStatus(String instanceId) {
        WorkflowInstance instance = instances.get(instanceId);
        if (instance == null) {
            throw new IllegalStateException("Unknown workflow instance: " + instanceId);
        }
        return instance;
    }

    /**
     * 供上层动态注册处理器。
     */
    public void registerProcessor(String name, TaskProcessor processor) {
        processorRegistry.register(name, processor);
    }

    /**
     * 注册事件监听器。
     *
     * @param listener 监听器
     */
    public void registerEventListener(WorkflowEventListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * 移除事件监听器。
     *
     * @param listener 监听器
     */
    public void removeEventListener(WorkflowEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知节点成功。
     *
     * @param instanceId 实例 ID
     * @param nodeId     节点 ID
     */
    public void onNodeSuccess(String instanceId, String nodeId) {
        listeners.forEach(listener -> listener.onNodeSuccess(instanceId, nodeId));
    }

    /**
     * 通知节点失败。
     *
     * @param instanceId 实例 ID
     * @param nodeId     节点 ID
     * @param error      失败原因
     */
    public void onNodeFailure(String instanceId, String nodeId, Throwable error) {
        listeners.forEach(listener -> listener.onNodeFailure(instanceId, nodeId, error));
    }

    /**
     * 优雅关闭所有组件。
     */
    @PreDestroy
    public void shutdown() {
        workflowExecutor.shutdown();
        log.info("WorkflowEngine shutdown");
    }

    private WorkflowInstance updateInstance(String instanceId, Function<WorkflowInstance, WorkflowInstance> updater) {
        Objects.requireNonNull(updater, "updater");
        WorkflowInstance updated = instances.compute(instanceId, (id, current) -> {
            if (current == null) {
                throw new IllegalStateException("Unknown workflow instance: " + id);
            }
            return updater.apply(current);
        });
        if (updated == null) {
            throw new IllegalStateException("Workflow instance update failed: " + instanceId);
        }
        return updated;
    }

    private WorkflowDefinition requireDefinition(String definitionId) {
        WorkflowDefinition definition = definitions.get(definitionId);
        if (definition == null) {
            throw new IllegalStateException("Unknown workflow definition: " + definitionId);
        }
        return definition;
    }

    private void ensureStatus(WorkflowInstance instance, WorkflowInstance.Status expected) {
        if (instance.getStatus() != expected) {
            throw new IllegalStateException("Instance state invalid, expected " + expected + " but was " + instance.getStatus());
        }
    }

    private void notifyNodeStart(String instanceId, String nodeId) {
        listeners.forEach(listener -> listener.onNodeStart(instanceId, nodeId));
    }

    /**
     * 事件回调接口。
     */
    public interface WorkflowEventListener {

        void onNodeStart(String instanceId, String nodeId);

        void onNodeSuccess(String instanceId, String nodeId);

        void onNodeFailure(String instanceId, String nodeId, Throwable error);
    }
}
