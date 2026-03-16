package com.basebackend.album.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 家庭成员角色枚举
 *
 * @author BearTeam
 */
@Getter
@AllArgsConstructor
public enum FamilyRole {

    /** 普通成员 */
    MEMBER(0, "成员"),
    /** 管理员 */
    ADMIN(1, "管理员"),
    /** 创建者 */
    CREATOR(2, "创建者");

    private final int code;
    private final String description;
}
