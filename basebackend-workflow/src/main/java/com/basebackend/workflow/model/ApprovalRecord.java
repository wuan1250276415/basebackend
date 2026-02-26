package com.basebackend.workflow.model;

import java.time.Instant;

/**
 * 审批记录
 *
 * @param nodeId    节点 ID
 * @param nodeName  节点名称
 * @param approver  审批人
 * @param action    审批动作
 * @param comment   审批意见
 * @param timestamp 审批时间
 */
public record ApprovalRecord(
        String nodeId,
        String nodeName,
        String approver,
        ApprovalAction action,
        String comment,
        Instant timestamp
) {
    public enum ApprovalAction {
        /** 通过 */
        APPROVE,
        /** 驳回 */
        REJECT,
        /** 转交 */
        TRANSFER,
        /** 加签 */
        ADD_SIGN,
        /** 撤回 */
        WITHDRAW,
        /** 超时自动处理 */
        TIMEOUT
    }

    public static ApprovalRecord approve(String nodeId, String nodeName, String approver, String comment) {
        return new ApprovalRecord(nodeId, nodeName, approver, ApprovalAction.APPROVE, comment, Instant.now());
    }

    public static ApprovalRecord reject(String nodeId, String nodeName, String approver, String comment) {
        return new ApprovalRecord(nodeId, nodeName, approver, ApprovalAction.REJECT, comment, Instant.now());
    }

    public static ApprovalRecord transfer(String nodeId, String nodeName, String approver, String comment) {
        return new ApprovalRecord(nodeId, nodeName, approver, ApprovalAction.TRANSFER, comment, Instant.now());
    }

    public static ApprovalRecord timeout(String nodeId, String nodeName) {
        return new ApprovalRecord(nodeId, nodeName, "system", ApprovalAction.TIMEOUT, "审批超时，自动处理", Instant.now());
    }
}
