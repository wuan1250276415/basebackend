package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 好友状态枚举
 */
@Getter
@AllArgsConstructor
public enum FriendStatus {

    PENDING(0, "待验证"),
    NORMAL(1, "正常"),
    DELETED(2, "已删除");

    private final int code;
    private final String description;
}
