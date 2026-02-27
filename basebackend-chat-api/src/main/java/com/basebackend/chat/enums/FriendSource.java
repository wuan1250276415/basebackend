package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 好友添加来源枚举
 */
@Getter
@AllArgsConstructor
public enum FriendSource {

    SEARCH(0, "搜索"),
    GROUP(1, "群聊"),
    CARD(2, "名片"),
    QR_CODE(3, "扫码");

    private final int code;
    private final String description;
}
