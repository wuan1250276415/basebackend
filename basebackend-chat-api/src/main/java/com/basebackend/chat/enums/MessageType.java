package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {

    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    FILE(3, "文件"),
    VOICE(4, "语音"),
    VIDEO(5, "视频"),
    LOCATION(6, "位置"),
    CARD(7, "名片"),
    EMOJI(8, "表情"),
    SYSTEM(9, "系统通知"),
    REVOKE(10, "撤回"),
    MERGE_FORWARD(11, "合并转发");

    private final int code;
    private final String description;

    public static MessageType fromCode(int code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知消息类型: " + code);
    }
}
