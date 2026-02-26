package com.basebackend.workflow.model;

import java.time.Instant;
import java.util.*;

/**
 * 流程实例
 * <p>
 * 一个流程定义的运行时实例，记录当前执行状态、流程变量、审批历史等。
 */
public class ProcessInstance {

    private final String instanceId;
    private final String processKey;
    private final String initiator;
    private final Instant startTime;
    private ProcessStatus status;
    private String currentNodeId;
    private final Map<String, Object> variables;
    private final List<ApprovalRecord> approvalHistory;
    private Instant endTime;

    public ProcessInstance(String instanceId, String processKey, String initiator, String startNodeId) {
        this.instanceId = instanceId;
        this.processKey = processKey;
        this.initiator = initiator;
        this.startTime = Instant.now();
        this.status = ProcessStatus.RUNNING;
        this.currentNodeId = startNodeId;
        this.variables = new LinkedHashMap<>();
        this.approvalHistory = new ArrayList<>();
    }

    // --- Getters ---

    public String getInstanceId() { return instanceId; }
    public String getProcessKey() { return processKey; }
    public String getInitiator() { return initiator; }
    public Instant getStartTime() { return startTime; }
    public ProcessStatus getStatus() { return status; }
    public String getCurrentNodeId() { return currentNodeId; }
    public Map<String, Object> getVariables() { return variables; }
    public List<ApprovalRecord> getApprovalHistory() { return Collections.unmodifiableList(approvalHistory); }
    public Instant getEndTime() { return endTime; }

    // --- 状态操作 ---

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

    public boolean isRunning() {
        return status == ProcessStatus.RUNNING;
    }

    public boolean isFinished() {
        return status == ProcessStatus.COMPLETED || status == ProcessStatus.REJECTED
                || status == ProcessStatus.CANCELLED;
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
}
