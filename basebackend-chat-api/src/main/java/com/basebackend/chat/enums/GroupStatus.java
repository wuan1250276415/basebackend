package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群组状态枚举
 */
@Getter
@AllArgsConstructor
public enum GroupStatus {

    DISSOLVED(0, "已解散"),
    NORMAL(1, "正常"),
    BANNED(2, "封禁");

    private final int code;
    private final String description;
}
