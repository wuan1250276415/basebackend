package com.basebackend.album.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 可见性枚举
 *
 * @author BearTeam
 */
@Getter
@AllArgsConstructor
public enum Visibility {

    /** 私有 */
    PRIVATE(0, "私有"),
    /** 家庭可见 */
    FAMILY(1, "家庭"),
    /** 链接公开 */
    PUBLIC(2, "公开");

    private final int code;
    private final String description;
}
