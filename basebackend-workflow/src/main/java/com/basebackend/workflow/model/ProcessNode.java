package com.basebackend.workflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程节点
 */
public class ProcessNode {

    private final String id;
    private final String name;
    private final NodeType type;
    private final String assigneeRole;
    private final Long timeoutMinutes;
    private final List<ConditionBranch> branches;
    private final List<String> notifyRoles;
    private final List<String> nextNodeIds;

    public ProcessNode(String id, String name, NodeType type, String assigneeRole,
                       Long timeoutMinutes, List<ConditionBranch> branches, List<String> notifyRoles) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.assigneeRole = assigneeRole;
        this.timeoutMinutes = timeoutMinutes;
        this.branches = branches != null ? branches : List.of();
        this.notifyRoles = notifyRoles != null ? notifyRoles : List.of();
        this.nextNodeIds = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public NodeType getType() { return type; }
    public String getAssigneeRole() { return assigneeRole; }
    public Long getTimeoutMinutes() { return timeoutMinutes; }
    public List<ConditionBranch> getBranches() { return branches; }
    public List<String> getNotifyRoles() { return notifyRoles; }
    public List<String> getNextNodeIds() { return nextNodeIds; }

    public boolean isApproval() { return type == NodeType.APPROVAL; }
    public boolean isCondition() { return type == NodeType.CONDITION; }
    public boolean isEnd() { return type == NodeType.END; }
}
