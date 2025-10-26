package com.basebackend.generator.entity;

import lombok.Getter;

/**
 * 模板引擎类型枚举
 */
@Getter
public enum EngineType {
    
    FREEMARKER("FreeMarker", "ftl"),
    VELOCITY("Velocity", "vm"),
    THYMELEAF("Thymeleaf", "html");

    private final String displayName;
    private final String extension;

    EngineType(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }
}
