package com.basebackend.common.enums;

import lombok.Getter;

/**
 * 状态枚举
 */
@Getter
public enum StatusEnum {

    /**
     * 启用
     */
    ENABLED(1, "启用"),

    /**
     * 禁用
     */
    DISABLED(0, "禁用");

    private final Integer code;
    private final String message;

    StatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
