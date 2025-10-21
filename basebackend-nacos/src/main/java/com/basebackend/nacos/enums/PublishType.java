package com.basebackend.nacos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发布类型枚举
 */
@Getter
@AllArgsConstructor
public enum PublishType {

    /**
     * 自动发布
     */
    AUTO("auto", "自动发布"),

    /**
     * 手动发布
     */
    MANUAL("manual", "手动发布"),

    /**
     * 灰度发布
     */
    GRAY("gray", "灰度发布");

    private final String code;
    private final String description;

    public static PublishType fromCode(String code) {
        for (PublishType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
