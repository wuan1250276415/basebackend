package com.basebackend.workflow.model;

import java.time.Instant;
import java.util.*;

/**
 * 流程定义
 * <p>
 * 描述一个完整的审批流程结构：节点定义、条件分支、超时策略等。
 *
 * <pre>
 * ProcessDefinition def = ProcessDefinition.builder("leave_approval", "请假审批")
 *     .startNode("submit", "提交申请")
 *     .approvalNode("leader", "主管审批", "ROLE_LEADER")
 *     .conditionNode("days_check", "天数判断")
 *         .branch("days <= 3", "leader")
 *         .branch("days > 3", "hr")
 *     .approvalNode("hr", "HR审批", "ROLE_HR")
 *     .endNode("completed", "审批完成")
 *     .build();
 * </pre>
 */
public class ProcessDefinition {

    private final String processKey;
    private final String name;
    private final String description;
    private final int version;
    private final Map<String, ProcessNode> nodes;
    private final String startNodeId;
    private final Instant createdAt;

    private ProcessDefinition(String processKey, String name, String description, int version,
                               Map<String, ProcessNode> nodes, String startNodeId) {
        this.processKey = processKey;
        this.name = name;
        this.description = description;
        this.version = version;
        this.nodes = nodes;
        this.startNodeId = startNodeId;
        this.createdAt = Instant.now();
    }

    public static Builder builder(String processKey, String name) {
        return new Builder(processKey, name);
    }

    // --- Getters ---

    public String getProcessKey() { return processKey; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getVersion() { return version; }
    public Map<String, ProcessNode> getNodes() { return Collections.unmodifiableMap(nodes); }
    public String getStartNodeId() { return startNodeId; }
    public Instant getCreatedAt() { return createdAt; }

    public ProcessNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    // --- Builder ---

    public static class Builder {
        private final String processKey;
        private final String name;
        private String description = "";
        private int version = 1;
        private final Map<String, ProcessNode> nodes = new LinkedHashMap<>();
        private String startNodeId;

        Builder(String processKey, String name) {
            this.processKey = processKey;
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        /** 添加开始节点 */
        public Builder startNode(String id, String name) {
            this.startNodeId = id;
            nodes.put(id, new ProcessNode(id, name, NodeType.START, null, null, null, null));
            return this;
        }

        /** 添加审批节点 */
        public Builder approvalNode(String id, String name, String assigneeRole) {
            nodes.put(id, new ProcessNode(id, name, NodeType.APPROVAL, assigneeRole, null, null, null));
            return this;
        }

        /** 添加审批节点（带超时） */
        public Builder approvalNode(String id, String name, String assigneeRole, long timeoutMinutes) {
            nodes.put(id, new ProcessNode(id, name, NodeType.APPROVAL, assigneeRole, timeoutMinutes, null, null));
            return this;
        }

        /** 添加条件分支节点 */
        public Builder conditionNode(String id, String name, List<ConditionBranch> branches) {
            nodes.put(id, new ProcessNode(id, name, NodeType.CONDITION, null, null, branches, null));
            return this;
        }

        /** 添加抄送节点 */
        public Builder notifyNode(String id, String name, List<String> notifyRoles) {
            nodes.put(id, new ProcessNode(id, name, NodeType.NOTIFY, null, null, null, notifyRoles));
            return this;
        }

        /** 添加结束节点 */
        public Builder endNode(String id, String name) {
            nodes.put(id, new ProcessNode(id, name, NodeType.END, null, null, null, null));
            return this;
        }

        /** 设置节点间的转移（from → to） */
        public Builder transition(String fromNodeId, String toNodeId) {
            ProcessNode node = nodes.get(fromNodeId);
            if (node != null) {
                node.getNextNodeIds().add(toNodeId);
            }
            return this;
        }

        public ProcessDefinition build() {
            if (startNodeId == null) {
                throw new IllegalStateException("流程定义必须包含开始节点");
            }
            return new ProcessDefinition(processKey, name, description, version, nodes, startNodeId);
        }
    }
}
