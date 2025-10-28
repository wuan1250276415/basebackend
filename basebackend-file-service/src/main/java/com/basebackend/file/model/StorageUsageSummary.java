package com.basebackend.file.model;

import lombok.Data;

/**
 * 存储使用情况概览
 */
@Data
public class StorageUsageSummary {

    /**
     * 已使用空间（字节）
     */
    private long used;

    /**
     * 总空间（字节）
     */
    private long total;

    /**
     * 使用百分比（0-100）
     */
    private double percentage;
}
