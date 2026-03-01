package com.basebackend.ticket.enums;

/**
 * 工单评论类型枚举
 */
public enum CommentType {

    COMMENT("评论"),
    SYSTEM("系统消息"),
    APPROVAL("审批意见");

    private final String description;

    CommentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
