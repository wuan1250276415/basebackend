package com.basebackend.ticket.enums;

/**
 * 工单状态枚举
 */
public enum TicketStatus {

    OPEN("待处理"),
    IN_PROGRESS("处理中"),
    PENDING_APPROVAL("审批中"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    RESOLVED("已解决"),
    CLOSED("已关闭");

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 校验状态流转是否合法
     */
    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == PENDING_APPROVAL || target == CLOSED;
            case IN_PROGRESS -> target == PENDING_APPROVAL || target == RESOLVED || target == CLOSED;
            case PENDING_APPROVAL -> target == APPROVED || target == REJECTED || target == OPEN;
            case APPROVED -> target == RESOLVED || target == CLOSED;
            case REJECTED -> target == OPEN || target == CLOSED;
            case RESOLVED -> target == CLOSED || target == OPEN;
            case CLOSED -> false;
        };
    }
}
