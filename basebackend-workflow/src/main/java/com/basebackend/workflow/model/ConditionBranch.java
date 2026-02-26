package com.basebackend.workflow.model;

/**
 * 条件分支
 *
 * @param expression  条件表达式（如 "days <= 3"）
 * @param targetNodeId 满足条件时跳转的节点 ID
 */
public record ConditionBranch(String expression, String targetNodeId) {

    public static ConditionBranch of(String expression, String targetNodeId) {
        return new ConditionBranch(expression, targetNodeId);
    }
}
