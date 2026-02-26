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
 *   <li>审批动作的处理（通过/驳回/转交）</li>
 * </ul>
 *
 * 内存实现，生产环境可替换为数据库持久化实现。
 */
@Slf4j
public class ProcessEngine {

    /** processKey → ProcessDefinition */
    private final Map<String, ProcessDefinition> definitions = new ConcurrentHashMap<>();

    /** instanceId → ProcessInstance */
    private final Map<String, ProcessInstance> instances = new ConcurrentHashMap<>();

    /** 条件表达式求值器 */
    private final ConditionEvaluator conditionEvaluator;

    public ProcessEngine() {
        this.conditionEvaluator = new ConditionEvaluator();
    }

    // ==================== 流程定义管理 ====================

    /**
     * 注册流程定义
     */
    public void deploy(ProcessDefinition definition) {
        definitions.put(definition.getProcessKey(), definition);
        log.info("流程定义已部署: key={}, name={}, nodes={}",
                definition.getProcessKey(), definition.getName(), definition.getNodeCount());
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
     * @param variables  初始流程变量
     * @return 流程实例
     */
    public ProcessInstance startProcess(String processKey, String initiator, Map<String, Object> variables) {
        ProcessDefinition definition = definitions.get(processKey);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + processKey);
        }

        String instanceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ProcessInstance instance = new ProcessInstance(instanceId, processKey, initiator, definition.getStartNodeId());

        if (variables != null) {
            variables.forEach(instance::setVariable);
        }

        instances.put(instanceId, instance);
        log.info("流程实例已启动: instanceId={}, processKey={}, initiator={}", instanceId, processKey, initiator);

        // 自动推进到第一个有效节点
        advanceFromNode(instance, definition, definition.getStartNodeId());

        return instance;
    }

    /**
     * 审批通过
     */
    public ProcessInstance approve(String instanceId, String approver, String comment) {
        ProcessInstance instance = getRunningInstance(instanceId);
        ProcessDefinition definition = definitions.get(instance.getProcessKey());
        ProcessNode currentNode = definition.getNode(instance.getCurrentNodeId());

        instance.addApprovalRecord(
                ApprovalRecord.approve(currentNode.getId(), currentNode.getName(), approver, comment)
        );

        log.info("审批通过: instanceId={}, node={}, approver={}", instanceId, currentNode.getName(), approver);

        // 推进到下一个节点
        advanceToNext(instance, definition, currentNode);

        return instance;
    }

    /**
     * 审批驳回
     */
    public ProcessInstance reject(String instanceId, String approver, String comment) {
        ProcessInstance instance = getRunningInstance(instanceId);
        ProcessDefinition definition = definitions.get(instance.getProcessKey());
        ProcessNode currentNode = definition.getNode(instance.getCurrentNodeId());

        instance.addApprovalRecord(
                ApprovalRecord.reject(currentNode.getId(), currentNode.getName(), approver, comment)
        );

        instance.reject();
        log.info("审批驳回: instanceId={}, node={}, approver={}", instanceId, currentNode.getName(), approver);

        return instance;
    }

    /**
     * 转交任务
     */
    public ProcessInstance transfer(String instanceId, String fromApprover, String toApprover, String comment) {
        ProcessInstance instance = getRunningInstance(instanceId);
        ProcessDefinition definition = definitions.get(instance.getProcessKey());
        ProcessNode currentNode = definition.getNode(instance.getCurrentNodeId());

        instance.addApprovalRecord(
                ApprovalRecord.transfer(currentNode.getId(), currentNode.getName(), fromApprover,
                        "转交给 " + toApprover + ": " + comment)
        );

        log.info("任务转交: instanceId={}, from={}, to={}", instanceId, fromApprover, toApprover);

        return instance;
    }

    /**
     * 取消流程
     */
    public ProcessInstance cancel(String instanceId, String operator) {
        ProcessInstance instance = getRunningInstance(instanceId);
        instance.cancel();
        log.info("流程已取消: instanceId={}, operator={}", instanceId, operator);
        return instance;
    }

    /**
     * 获取流程实例
     */
    public ProcessInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }

    /**
     * 获取用户待办任务列表
     */
    public List<ProcessInstance> getTasksByRole(String role) {
        return instances.values().stream()
                .filter(ProcessInstance::isRunning)
                .filter(inst -> {
                    ProcessDefinition def = definitions.get(inst.getProcessKey());
                    if (def == null) return false;
                    ProcessNode node = def.getNode(inst.getCurrentNodeId());
                    return node != null && node.isApproval() && role.equals(node.getAssigneeRole());
                })
                .toList();
    }

    /**
     * 获取用户发起的流程列表
     */
    public List<ProcessInstance> getMyProcesses(String initiator) {
        return instances.values().stream()
                .filter(inst -> initiator.equals(inst.getInitiator()))
                .toList();
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
                    log.warn("条件分支无匹配: instanceId={}, nodeId={}", instance.getInstanceId(), nodeId);
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

    private ProcessInstance getRunningInstance(String instanceId) {
        ProcessInstance instance = instances.get(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + instanceId);
        }
        if (!instance.isRunning()) {
            throw new IllegalStateException("流程实例已结束: " + instanceId + ", status=" + instance.getStatus());
        }
        return instance;
    }
}
