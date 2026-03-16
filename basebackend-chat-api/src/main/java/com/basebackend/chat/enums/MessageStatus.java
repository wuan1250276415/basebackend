package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举
 */
@Getter
@AllArgsConstructor
public enum MessageStatus {

    SENDING(0, "发送中"),
    SENT(1, "已发送"),
    REVOKED(2, "已撤回"),
    AUDITING(3, "审核中"),
    BLOCKED(4, "已屏蔽");

    private final int code;
    private final String description;

    public static MessageStatus fromCode(int code) {
        for (MessageStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知消息状态: " + code);
    }
}
