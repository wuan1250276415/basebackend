/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package com.basebackend.nacos.enums;

import lombok.Generated;

public enum GrayStrategyType {
    IP("ip", "\u6309IP\u7070\u5ea6"),
    PERCENTAGE("percentage", "\u6309\u767e\u5206\u6bd4\u7070\u5ea6"),
    LABEL("label", "\u6309\u6807\u7b7e\u7070\u5ea6");

    private final String code;
    private final String description;

    public static GrayStrategyType fromCode(String code) {
        for (GrayStrategyType type : GrayStrategyType.values()) {
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
    private GrayStrategyType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

