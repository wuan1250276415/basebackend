package com.basebackend.nacos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配置状态枚举
 */
@Getter
@AllArgsConstructor
public enum ConfigStatus {

    /**
     * 草稿
     */
    DRAFT("draft", "草稿"),

    /**
     * 已发布
     */
    PUBLISHED("published", "已发布"),

    /**
     * 已归档
     */
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String description;

    public static ConfigStatus fromCode(String code) {
        for (ConfigStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
