package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.nio.file.attribute.BasicFileAttributes;

/**
 * PostgreSQL WAL文件信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalFileInfo {

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件名
     */
    private String name;

    /**
     * 文件大小
     */
    private long size;

    /**
     * 创建时间
     */
    private BasicFileAttributes createdTime;

    /**
     * 文件是否有效
     */
    private boolean valid;

    /**
     * 文件序号（WAL文件编号）
     */
    private String sequence;

    /**
     * 获取人类可读的文件大小
     */
    public String getSizeHumanReadable() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }
}
