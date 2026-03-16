package com.basebackend.ticket.enums;

/**
 * 审批动作枚举
 */
public enum ApprovalAction {

    APPROVE("通过"),
    REJECT("拒绝"),
    RETURN("退回"),
    DELEGATE("转办"),
    COUNTERSIGN("加签");

    private final String description;

    ApprovalAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
