package com.basebackend.nacos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 灰度策略类型枚举
 */
@Getter
@AllArgsConstructor
public enum GrayStrategyType {

    /**
     * 按IP灰度
     */
    IP("ip", "按IP灰度"),

    /**
     * 按百分比灰度
     */
    PERCENTAGE("percentage", "按百分比灰度"),

    /**
     * 按标签灰度
     */
    LABEL("label", "按标签灰度");

    private final String code;
    private final String description;

    public static GrayStrategyType fromCode(String code) {
        for (GrayStrategyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
