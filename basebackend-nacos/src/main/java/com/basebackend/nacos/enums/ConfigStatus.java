/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.enums;

import lombok.Generated;

public enum ConfigStatus {
    DRAFT("draft", "\u8349\u7a3f"),
    PUBLISHED("published", "\u5df2\u53d1\u5e03"),
    ARCHIVED("archived", "\u5df2\u5f52\u6863");

    private final String code;
    private final String description;

    public static ConfigStatus fromCode(String code) {
        for (ConfigStatus status : ConfigStatus.values()) {
            if (!status.code.equals(code)) continue;
            return status;
        }
        return null;
    }

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    private ConfigStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

