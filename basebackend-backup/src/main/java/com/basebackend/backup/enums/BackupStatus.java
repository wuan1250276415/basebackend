package com.basebackend.backup.enums;

import lombok.Getter;

/**
 * 备份状态
 *
 * @author BaseBackend
 */
@Getter
public enum BackupStatus {

    /**
     * 备份中
     */
    RUNNING("running", "备份中"),

    /**
     * 备份成功
     */
    SUCCESS("success", "备份成功"),

    /**
     * 备份失败
     */
    FAILED("failed", "备份失败");

    private final String code;
    private final String description;

    BackupStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
