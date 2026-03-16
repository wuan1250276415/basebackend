package com.basebackend.album.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分享链接状态枚举
 *
 * @author BearTeam
 */
@Getter
@AllArgsConstructor
public enum ShareStatus {

    /** 已失效 */
    EXPIRED(0, "已失效"),
    /** 有效 */
    ACTIVE(1, "有效");

    private final int code;
    private final String description;
}
