package com.basebackend.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 群成员角色枚举
 */
@Getter
@AllArgsConstructor
public enum GroupRole {

    MEMBER(0, "普通成员"),
    ADMIN(1, "管理员"),
    OWNER(2, "群主");

    private final int code;
    private final String description;

    /** 判断是否有管理权限（管理员或群主） */
    public boolean hasAdminPrivilege() {
        return this == ADMIN || this == OWNER;
    }
}
