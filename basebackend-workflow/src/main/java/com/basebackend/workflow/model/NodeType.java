package com.basebackend.workflow.model;

/**
 * 节点类型
 */
public enum NodeType {
    /** 开始节点 */
    START,
    /** 审批节点 */
    APPROVAL,
    /** 条件分支节点 */
    CONDITION,
    /** 抄送通知节点 */
    NOTIFY,
    /** 结束节点 */
    END
}
