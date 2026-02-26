package com.basebackend.workflow.model;

/**
 * 流程实例状态
 */
public enum ProcessStatus {
    /** 运行中 */
    RUNNING,
    /** 已完成（审批通过） */
    COMPLETED,
    /** 已驳回 */
    REJECTED,
    /** 已取消 */
    CANCELLED,
    /** 已挂起 */
    SUSPENDED
}
