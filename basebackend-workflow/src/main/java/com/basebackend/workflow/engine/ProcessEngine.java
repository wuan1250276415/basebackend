package com.basebackend.workflow.engine;

import com.basebackend.workflow.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程引擎
 * <p>
 * 工作流的核心执行引擎，负责：
 * <ul>
 *   <li>流程定义注册和管理</li>
 *   <li>流程实例的创建和推进</li>
 *   <li>条件分支的计算</li>
 *   <li>审批动作的处理（通过/驳回/转交/取消）</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * <p>
 * 对同一实例的并发审批操作通过 {@code synchronized(instance)} 保证原子性，
 * 防止 check-then-act 竞态条件。实例内部字段使用 volatile 和并发容器保证可见性。
 *
 * <h3>生命周期</h3>
 * <p>
 * 已结束实例不会自动清理，需调用 {@link #cleanupFinished()} 防止内存持续增长。
 *
 * <p>内存实现，生产环境可替换为数据库持久化实现。
 */
@Slf4j
public class ProcessEngine {

    /** processKey → ProcessDefinition */
    private final Map<String, ProcessDefinition> definitions = new ConcurrentHashMap<>();

    /** instanceId → ProcessInstance */
    private final Map<String, ProcessInstance> instances = new ConcurrentHashMap<>();

    /** 条件表达式求值器 */
    private final ConditionEvaluator conditionEvaluator;

    /**
     * 审批权限校验器（可选）。
     * 为 null 时跳过权限校验，适用于测试或不需要权限控制的场景。
     */
    private final RoleChecker roleChecker;

    public ProcessEngine() {
        this.conditionEvaluator = new ConditionEvaluator();
        this.roleChecker = null;
    }

    /**
     * @param roleChecker 权限校验器，由消费方注入；为 null 时禁用权限校验
     */
    public ProcessEngine(RoleChecker roleChecker) {
        this.conditionEvaluator = new ConditionEvaluator();
        this.roleChecker = roleChecker;
    }

    // ==================== 流程定义管理 ====================

    /**
     * 注册流程定义
     * <p>
     * 版本保护规则：若目标 processKey 已有流程定义且存在运行中的实例，
     * 新版本号必须大于当前版本号，否则拒绝部署（防止意外覆盖破坏进行中的流程）。
     *
     * @throws IllegalStateException 存在运行中实例且版本号未递增
     */
    public void deploy(ProcessDefinition definition) {
        Objects.requireNonNull(definition, "流程定义不能为空");

        ProcessDefinition existing = definitions.get(definition.getProcessKey());
        if (existing != null) {
            long runningCount = instances.values().stream()
                    .filter(inst -> definition.getProcessKey().equals(inst.getProcessKey()) && inst.isRunning())
                    .count();
            if (runningCount > 0 && definition.getVersion() <= existing.getVersion()) {
                throw new IllegalStateException(String.format(
                        "流程[%s]存在 %d 个运行中的实例，重新部署时版本号必须递增（当前: v%d，新版: v%d）",
                        definition.getProcessKey(), runningCount, existing.getVersion(), definition.getVersion()));
            }
            if (runningCount > 0) {
                log.warn("流程[{}]存在 {} 个运行中的实例，已更新至 v{}，运行中实例将沿用新定义",
                        definition.getProcessKey(), runningCount, definition.getVersion());
            }
        }

        definitions.put(definition.getProcessKey(), definition);
        log.info("流程定义已部署: key={}, name={}, version=v{}, nodes={}",
                definition.getProcessKey(), definition.getName(), definition.getVersion(), definition.getNodeCount());
    }

    /**
     * 获取流程定义
     */
    public ProcessDefinition getDefinition(String processKey) {
        return definitions.get(processKey);
    }

    /**
     * 获取所有流程定义
     */
    public Collection<ProcessDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    // ==================== 流程实例管理 ====================

    /**
     * 启动流程实例
     *
     * @param processKey 流程定义 key
     * @param initiator  发起人
     * @param variables  初始流程变量（可为 null）
     * @return 流程实例
     * @throws IllegalArgumentException 流程定义不存在
     * @throws IllegalStateException    条件分支无匹配（流程定义缺少 default 分支）
     */
    public ProcessInstance startProcess(String processKey, String initiator, Map<String, Object> variables) {
        Objects.requireNonNull(processKey, "processKey 不能为空");
        Objects.requireNonNull(initiator, "发起人不能为空");

        ProcessDefinition definition = definitions.get(processKey);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + processKey);
        }

        String instanceId = generateUniqueInstanceId();
        ProcessInstance instance = new ProcessInstance(instanceId, processKey, initiator, definition.getStartNodeId());

        if (variables != null) {
            variables.forEach(instance::setVariable);
        }

        // 先推进到第一个有效节点，再注册到引擎
        // 若推进失败（如条件无匹配），实例不入库，异常直接抛出给调用方
        advanceFromNode(instance, definition, definition.getStartNodeId());
        instances.put(instanceId, instance);
        log.info("流程实例已启动: instanceId={}, processKey={}, initiator={}", instanceId, processKey, initiator);

        return instance;
    }

    /**
     * 审批通过
     */
    public ProcessInstance approve(String instanceId, String approver, String comment) {
        Objects.requireNonNull(instanceId, "instanceId 不能为空");
        Objects.requireNonNull(approver, "审批人不能为空");

        ProcessInstance instance = getInstanceById(instanceId);
        synchronized (instance) {
            checkRunning(instance, instanceId);
            ProcessDefinition definition = definitions.get(instance.getProcessKey());
            ProcessNode currentNode = definition.getNode(instance.getCurrentNodeId());
            checkAuthorized(instance, currentNode, approver);

            instance.addApprovalRecord(
                    ApprovalRecord.approve(currentNode.getId(), currentNode.getName(), approver, comment)
            );
            log.info("审批通过: instanceId={}, node={}, approver={}", instanceId, currentNode.getName(), approver);
            advanceToNext(instance, definition, currentNode);
        }
        return instance;
    }

    /**
     * 审批驳回
     */
    public ProcessInstance reject(String instanceId, String approver, String comment) {
        Objects.requireNonNull(instanceId, "instanceId 不能为空");
        Objects.requireNonNull(approver, "审批人不能为空");

        ProcessInstance instance = getInstanceById(instanceId);
        synchronized (instance) {
            checkRunning(instance, instanceId);
            ProcessDefinition definition = definitions.get(instance.getProcessKey());
            ProcessNode currentNode = definition.getNode(instance.getCurrentNodeId());
            checkAuthorized(instance, currentNode, approver);

            instance.addApprovalRecord(
                    ApprovalRecord.reject(currentNode.getId(), currentNode.getName(), approver, comment)
            );
            instance.reject();
            log.info("审批驳回: instanceId={}, node={}, approver={}", instanceId, currentNode.getName(), approver);
        }
        return instance;
    }

    /**
     * 转交任务
     * <p>
     * 转交后，任务不再归属原节点角色，而是归属指定用户。
     * 转交人可通过 {@link #getTasksByUser(String)} 查询待办。
     */
    public ProcessInstance transfer(String instanceId, String fromApprover, String toApprover, String comment) {
        Objects.requireNonNull(instanceId, "instanceId 不能为空");
        Objects.requireNonNull(fromApprover, "转交人不能为空");
        Objects.requireNonNull(toApprover, "被转交人不能为空");

        ProcessInstance instance = getInstanceById(instanceId);
        synchronized (instance) {
            checkRunning(instance, instanceId);
            ProcessDefinition definition = definitions.get(instance.getProcessKey());
            ProcessNode currentNode = definition.getNode(instance.getCurrentNodeId());
            checkAuthorized(instance, currentNode, fromApprover);

            instance.addApprovalRecord(
                    ApprovalRecord.transfer(currentNode.getId(), currentNode.getName(), fromApprover,
                            "转交给 " + toApprover + ": " + comment)
            );
            // 记录覆盖：被转交人接管当前节点，原角色不再可见此任务
            instance.setNodeAssigneeOverride(instance.getCurrentNodeId(), toApprover);
            log.info("任务转交: instanceId={}, from={}, to={}", instanceId, fromApprover, toApprover);
        }
        return instance;
    }

    /**
     * 取消流程
     */
    public ProcessInstance cancel(String instanceId, String operator) {
        Objects.requireNonNull(instanceId, "instanceId 不能为空");
        Objects.requireNonNull(operator, "操作人不能为空");

        ProcessInstance instance = getInstanceById(instanceId);
        synchronized (instance) {
            checkRunning(instance, instanceId);
            instance.cancel();
            log.info("流程已取消: instanceId={}, operator={}", instanceId, operator);
        }
        return instance;
    }

    /**
     * 获取流程实例
     */
    public ProcessInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }

    /**
     * 获取角色的待办任务列表
     * <p>
     * 已被转交的任务不会出现在此列表中，转交后任务通过 {@link #getTasksByUser(String)} 查询。
     */
    public List<ProcessInstance> getTasksByRole(String role) {
        Objects.requireNonNull(role, "角色不能为空");
        return instances.values().stream()
                .filter(ProcessInstance::isRunning)
                .filter(inst -> {
                    ProcessDefinition def = definitions.get(inst.getProcessKey());
                    if (def == null) return false;
                    ProcessNode node = def.getNode(inst.getCurrentNodeId());
                    if (node == null || !node.isApproval()) return false;
                    // 当前节点已被转交，不出现在角色待办中
                    if (inst.getNodeAssigneeOverride(inst.getCurrentNodeId()) != null) return false;
                    return role.equals(node.getAssigneeRole());
                })
                .toList();
    }

    /**
     * 获取指定用户（通过转交获得）的待办任务列表
     */
    public List<ProcessInstance> getTasksByUser(String user) {
        Objects.requireNonNull(user, "用户名不能为空");
        return instances.values().stream()
                .filter(ProcessInstance::isRunning)
                .filter(inst -> user.equals(inst.getNodeAssigneeOverride(inst.getCurrentNodeId())))
                .toList();
    }

    /**
     * 获取用户发起的流程列表
     */
    public List<ProcessInstance> getMyProcesses(String initiator) {
        Objects.requireNonNull(initiator, "发起人不能为空");
        return instances.values().stream()
                .filter(inst -> initiator.equals(inst.getInitiator()))
                .toList();
    }

    /**
     * 清理已结束的流程实例（COMPLETED / REJECTED / CANCELLED / ERROR）
     * <p>
     * 内存实现不会自动释放已结束实例，建议在应用层通过定时任务定期调用此方法，防止内存泄漏。
     *
     * @return 本次清理的实例数量
     */
    public int cleanupFinished() {
        int[] count = {0};
        instances.entrySet().removeIf(entry -> {
            if (entry.getValue().isFinished()) {
                count[0]++;
                return true;
            }
            return false;
        });
        if (count[0] > 0) {
            log.info("清理已结束流程实例: {}个", count[0]);
        }
        return count[0];
    }

    // ==================== 内部流程推进 ====================

    private void advanceFromNode(ProcessInstance instance, ProcessDefinition definition, String nodeId) {
        ProcessNode node = definition.getNode(nodeId);
        if (node == null) return;

        switch (node.getType()) {
            case START -> {
                if (!node.getNextNodeIds().isEmpty()) {
                    String nextId = node.getNextNodeIds().getFirst();
                    instance.moveTo(nextId);
                    advanceFromNode(instance, definition, nextId);
                }
            }
            case CONDITION -> {
                String targetNodeId = conditionEvaluator.evaluate(node.getBranches(), instance.getVariables());
                if (targetNodeId != null) {
                    instance.moveTo(targetNodeId);
                    advanceFromNode(instance, definition, targetNodeId);
                } else {
                    // 标记实例错误状态后抛出，提醒调用方检查流程定义
                    instance.markError();
                    throw new IllegalStateException(String.format(
                            "流程条件分支无匹配: instanceId=%s, nodeId=%s，请为条件节点添加 default 分支",
                            instance.getInstanceId(), nodeId));
                }
            }
            case NOTIFY -> {
                log.info("抄送通知: instanceId={}, node={}, roles={}",
                        instance.getInstanceId(), node.getName(), node.getNotifyRoles());
                if (!node.getNextNodeIds().isEmpty()) {
                    String nextId = node.getNextNodeIds().getFirst();
                    instance.moveTo(nextId);
                    advanceFromNode(instance, definition, nextId);
                }
            }
            case END -> {
                instance.complete();
                log.info("流程完成: instanceId={}", instance.getInstanceId());
            }
            case APPROVAL -> {
                // 停在审批节点，等待人工操作
                log.info("等待审批: instanceId={}, node={}, assignee={}",
                        instance.getInstanceId(), node.getName(), node.getAssigneeRole());
            }
        }
    }

    private void advanceToNext(ProcessInstance instance, ProcessDefinition definition, ProcessNode currentNode) {
        if (!currentNode.getNextNodeIds().isEmpty()) {
            String nextId = currentNode.getNextNodeIds().getFirst();
            instance.moveTo(nextId);
            advanceFromNode(instance, definition, nextId);
        } else {
            // 无后续节点，流程完成
            instance.complete();
        }
    }

    /**
     * 生成唯一的实例 ID（32 位 UUID hex，含碰撞检测）
     */
    private String generateUniqueInstanceId() {
        String id;
        do {
            id = UUID.randomUUID().toString().replace("-", "");
        } while (instances.containsKey(id));
        return id;
    }

    private ProcessInstance getInstanceById(String instanceId) {
        ProcessInstance instance = instances.get(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + instanceId);
        }
        return instance;
    }

    private void checkRunning(ProcessInstance instance, String instanceId) {
        if (!instance.isRunning()) {
            throw new IllegalStateException(
                    "流程实例已结束: " + instanceId + ", status=" + instance.getStatus());
        }
    }

    /**
     * 权限校验：未注入 {@link RoleChecker} 时跳过。
     * <p>
     * 校验逻辑：
     * <ol>
     *   <li>节点已被转交 → 仅被转交人本人可操作</li>
     *   <li>节点未被转交 → 操作人须具备节点要求的角色</li>
     * </ol>
     *
     * @throws SecurityException 操作人无权操作当前节点
     */
    private void checkAuthorized(ProcessInstance instance, ProcessNode node, String operator) {
        if (roleChecker == null) return;

        String override = instance.getNodeAssigneeOverride(node.getId());
        boolean authorized;
        if (override != null) {
            // 已转交：只有被转交人可操作
            authorized = override.equals(operator);
        } else {
            // 未转交：操作人须具备节点角色
            authorized = node.getAssigneeRole() == null || roleChecker.hasRole(operator, node.getAssigneeRole());
        }

        if (!authorized) {
            throw new SecurityException(String.format(
                    "用户[%s]无权操作节点[%s]（需要角色: %s）", operator, node.getName(), node.getAssigneeRole()));
        }
    }
}
