/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.enums;

import lombok.Generated;

public enum PublishType {
    AUTO("auto", "\u81ea\u52a8\u53d1\u5e03"),
    MANUAL("manual", "\u624b\u52a8\u53d1\u5e03"),
    GRAY("gray", "\u7070\u5ea6\u53d1\u5e03");

    private final String code;
    private final String description;

    public static PublishType fromCode(String code) {
        for (PublishType type : PublishType.values()) {
            if (!type.code.equals(code)) continue;
            return type;
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
    private PublishType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

