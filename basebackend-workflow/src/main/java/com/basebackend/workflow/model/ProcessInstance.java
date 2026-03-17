package com.basebackend.workflow.model;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 流程实例
 * <p>
 * 一个流程定义的运行时实例，记录当前执行状态、流程变量、审批历史等。
 * <p>
 * 线程安全说明：状态字段（status、currentNodeId、endTime）使用 volatile 保证可见性；
 * 集合字段使用并发容器；状态变更方法由 {@link com.basebackend.workflow.engine.ProcessEngine}
 * 通过 synchronized(instance) 保证原子性。
 */
public class ProcessInstance {

    private final String instanceId;
    private final String processKey;
    private final String initiator;
    private final Instant startTime;

    /** volatile 保证多线程可见性 */
    private volatile ProcessStatus status;
    private volatile String currentNodeId;
    private volatile Instant endTime;

    /** ConcurrentHashMap 保证变量读写线程安全 */
    private final Map<String, Object> variables;

    /** CopyOnWriteArrayList 保证审批历史读多写少场景下的线程安全 */
    private final List<ApprovalRecord> approvalHistory;

    /** 转交覆盖：nodeId → 被转交的具体用户名，覆盖节点原始角色分配 */
    private final Map<String, String> nodeAssigneeOverrides;

    public ProcessInstance(String instanceId, String processKey, String initiator, String startNodeId) {
        this.instanceId = instanceId;
        this.processKey = processKey;
        this.initiator = initiator;
        this.startTime = Instant.now();
        this.status = ProcessStatus.RUNNING;
        this.currentNodeId = startNodeId;
        this.variables = new ConcurrentHashMap<>();
        this.approvalHistory = new CopyOnWriteArrayList<>();
        this.nodeAssigneeOverrides = new ConcurrentHashMap<>();
    }

    // --- Getters ---

    public String getInstanceId() { return instanceId; }
    public String getProcessKey() { return processKey; }
    public String getInitiator() { return initiator; }
    public Instant getStartTime() { return startTime; }
    public ProcessStatus getStatus() { return status; }
    public String getCurrentNodeId() { return currentNodeId; }
    public Instant getEndTime() { return endTime; }

    /** 返回流程变量的不可变视图，防止外部修改影响引擎内部状态 */
    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public List<ApprovalRecord> getApprovalHistory() {
        return Collections.unmodifiableList(approvalHistory);
    }

    // --- 状态操作（由 ProcessEngine 在 synchronized 块内调用）---

    public void moveTo(String nodeId) {
        this.currentNodeId = nodeId;
    }

    public void complete() {
        this.status = ProcessStatus.COMPLETED;
        this.endTime = Instant.now();
    }

    public void reject() {
        this.status = ProcessStatus.REJECTED;
        this.endTime = Instant.now();
    }

    public void cancel() {
        this.status = ProcessStatus.CANCELLED;
        this.endTime = Instant.now();
    }

    public void suspend() {
        this.status = ProcessStatus.SUSPENDED;
    }

    public void resume() {
        this.status = ProcessStatus.RUNNING;
    }

    /** 标记流程进入异常状态（如条件分支无匹配） */
    public void markError() {
        this.status = ProcessStatus.ERROR;
        this.endTime = Instant.now();
    }

    public boolean isRunning() {
        return status == ProcessStatus.RUNNING;
    }

    public boolean isFinished() {
        ProcessStatus s = status;
        return s == ProcessStatus.COMPLETED || s == ProcessStatus.REJECTED
                || s == ProcessStatus.CANCELLED || s == ProcessStatus.ERROR;
    }

    // --- 变量操作 ---

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public <T> T getVariable(String key, Class<T> type) {
        Object value = variables.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    // --- 审批记录 ---

    public void addApprovalRecord(ApprovalRecord record) {
        approvalHistory.add(record);
    }

    // --- 转交覆盖 ---

    /**
     * 设置当前节点的审批人覆盖（转交时使用）。
     * 设置后，该节点不再归属原角色，而是归属指定用户。
     */
    public void setNodeAssigneeOverride(String nodeId, String assignee) {
        nodeAssigneeOverrides.put(nodeId, assignee);
    }

    /**
     * 获取节点的审批人覆盖。
     * 返回 null 表示未被转交，仍由原角色负责。
     */
    public String getNodeAssigneeOverride(String nodeId) {
        return nodeAssigneeOverrides.get(nodeId);
    }
}
