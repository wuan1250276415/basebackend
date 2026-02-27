package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 好友申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum FriendRequestStatus {

    PENDING(0, "待处理"),
    ACCEPTED(1, "已同意"),
    REJECTED(2, "已拒绝"),
    EXPIRED(3, "已过期");

    private final int code;
    private final String description;
}
