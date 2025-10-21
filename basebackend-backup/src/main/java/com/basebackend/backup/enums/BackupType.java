package com.basebackend.backup.enums;

import lombok.Getter;

/**
 * 备份类型
 *
 * @author BaseBackend
 */
@Getter
public enum BackupType {

    /**
     * 全量备份
     */
    FULL("full", "全量备份"),

    /**
     * 增量备份
     */
    INCREMENTAL("incremental", "增量备份");

    private final String code;
    private final String description;

    BackupType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
