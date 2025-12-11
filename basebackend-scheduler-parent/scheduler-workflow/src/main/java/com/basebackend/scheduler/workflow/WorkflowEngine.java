package com.basebackend.scheduler.workflow;

import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.config.SchedulerCoreProperties;
import com.basebackend.scheduler.processor.ProcessorRegistry;
import com.basebackend.scheduler.engine.WorkflowExecutor;
import com.basebackend.scheduler.persistence.service.WorkflowPersistenceService;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Instant;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 工作流引擎入口，负责接收定义与实例并委托 WorkflowExecutor 运行。
 */
@Component
@ConditionalOnBean(WorkflowExecutor.class)
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    // 默认缓存配置常量
    private static final int DEFAULT_DEFINITION_CACHE_SIZE = 1000;
    private static final Duration DEFAULT_DEFINITION_CACHE_TTL = Duration.ofHours(1);
    private static final int DEFAULT_INSTANCE_CACHE_SIZE = 10000;
    private static final Duration DEFAULT_INSTANCE_CACHE_TTL = Duration.ofMinutes(30);

    // 工作流定义缓存
    private final LoadingCache<String, WorkflowDefinition> definitions;
    // 工作流实例缓存
    private final LoadingCache<String, WorkflowInstance> instances;
    private final CopyOnWriteArrayList<WorkflowEventListener> listeners;
    private final WorkflowExecutor workflowExecutor;
    private final ProcessorRegistry processorRegistry;
    private final WorkflowPersistenceService persistenceService;

    public WorkflowEngine(WorkflowExecutor workflowExecutor,
                          ProcessorRegistry processorRegistry,
                          WorkflowPersistenceService persistenceService,
                          @Autowired(required = false) SchedulerCoreProperties properties) {
        // 使用配置属性或默认值
        int definitionMaxSize = properties != null ? properties.getCache().getDefinitionMaxSize() : DEFAULT_DEFINITION_CACHE_SIZE;
        Duration definitionTtl = properties != null ? properties.getCache().getDefinitionExpireAfterAccess() : DEFAULT_DEFINITION_CACHE_TTL;
        int instanceMaxSize = properties != null ? properties.getCache().getInstanceMaxSize() : DEFAULT_INSTANCE_CACHE_SIZE;
        Duration instanceTtl = properties != null ? properties.getCache().getInstanceExpireAfterAccess() : DEFAULT_INSTANCE_CACHE_TTL;

        this.definitions = Caffeine.newBuilder()
                .maximumSize(definitionMaxSize)
                .expireAfterAccess(definitionTtl)
                .recordStats()
                .build(key -> {
                    throw new IllegalStateException("Workflow definition not found: " + key);
                });

        this.instances = Caffeine.newBuilder()
                .maximumSize(instanceMaxSize)
                .expireAfterAccess(instanceTtl)
                .recordStats()
                .build(key -> {
                    throw new IllegalStateException("Workflow instance not found: " + key);
                });

        this.listeners = new CopyOnWriteArrayList<>();
        this.workflowExecutor = workflowExecutor;
        this.processorRegistry = processorRegistry;
        this.persistenceService = persistenceService;

        log.info("WorkflowEngine 初始化完成 - 定义缓存: maxSize={}, ttl={}, 实例缓存: maxSize={}, ttl={}",
                definitionMaxSize, definitionTtl, instanceMaxSize, instanceTtl);

        // 启动时恢复运行中的实例
        recoverRunningInstances();
    }

    /**
     * 启动时恢复运行中的实例
     */
    private void recoverRunningInstances() {
        try {
            // TODO: 从数据库恢复所有运行中的实例
            // 这里可以根据需要实现恢复逻辑
            log.info("Workflow persistence recovery initialized");
        } catch (Exception e) {
            log.error("Failed to recover running instances", e);
        }
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
        WorkflowDefinition previous = definitions.asMap().putIfAbsent(definition.getId(), definition);
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
        instances.asMap().put(instanceId, instance);

        // 持久化实例状态
        try {
            persistenceService.save(instance);
            log.info("Workflow instance [{}] started and persisted", instanceId);
        } catch (Exception e) {
            log.error("Failed to persist workflow instance [{}]", instanceId, e);
            // 不抛出异常，允许实例在内存中继续运行
        }

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
        WorkflowInstance updated = instances.asMap().compute(instanceId, (id, current) -> {
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
        WorkflowDefinition definition = definitions.asMap().get(definitionId);
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

    // ========== 持久化相关方法 ==========

    /**
     * 恢复工作流实例
     */
    public Optional<WorkflowInstance> restoreWorkflowInstance(String instanceId) {
        return persistenceService.restore(instanceId)
                .map(instance -> {
                    // 将恢复的实例放入缓存
                    instances.asMap().put(instanceId, instance);
                    log.info("Restored workflow instance [{}] from persistence", instanceId);
                    return instance;
                });
    }

    /**
     * 更新实例状态
     */
    public boolean updateWorkflowStatus(String instanceId, WorkflowInstance.Status status, String errorMessage) {
        boolean success = persistenceService.updateStatus(instanceId, status, Instant.now(), errorMessage);
        if (success) {
            // 同时更新内存缓存
            WorkflowInstance current = instances.asMap().get(instanceId);
            if (current != null) {
                WorkflowInstance updated = current.withStatus(status, errorMessage, Instant.now(), current.getActiveNodes());
                instances.asMap().put(instanceId, updated);
            }
        }
        return success;
    }

    /**
     * 持久化当前实例状态
     */
    public void persistWorkflowInstance(String instanceId) {
        WorkflowInstance instance = instances.asMap().get(instanceId);
        if (instance != null) {
            try {
                persistenceService.save(instance);
                log.debug("Persisted workflow instance [{}]", instanceId);
            } catch (Exception e) {
                log.error("Failed to persist workflow instance [{}]", instanceId, e);
            }
        }
    }

    /**
     * 清理过期实例
     */
    public void cleanupExpiredInstances(Duration expireDuration) {
        Instant expireTime = Instant.now().minus(expireDuration);
        int count = persistenceService.cleanupExpiredInstances(expireTime);
        if (count > 0) {
            log.info("Cleaned up [{}] expired workflow instances", count);
        }
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
