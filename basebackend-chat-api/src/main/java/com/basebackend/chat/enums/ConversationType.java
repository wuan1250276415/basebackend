package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话类型枚举
 */
@Getter
@AllArgsConstructor
public enum ConversationType {

    PRIVATE(1, "私聊"),
    GROUP(2, "群聊");

    private final int code;
    private final String description;

    public static ConversationType fromCode(int code) {
        for (ConversationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知会话类型: " + code);
    }
}
