package com.basebackend.backup.infrastructure.storage;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 存储使用量统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsage {
    /**
     * 已使用的存储空间（字节）
     */
    private Long usedBytes;

    /**
     * 总配额空间（字节，-1表示无限制）
     */
    private Long totalBytes;

    /**
     * 已使用的存储空间（人类可读格式）
     */
    private String usedHumanReadable;

    /**
     * 总配额空间（人类可读格式）
     */
    private String totalHumanReadable;

    /**
     * 使用百分比
     */
    private Double usedPercentage;

    /**
     * 对象数量
     */
    private Long objectCount;

    /**
     * 存储桶/容器数量
     */
    private Long bucketCount;

    /**
     * 存储类型
     */
    private String storageType;

    /**
     * 区域
     */
    private String region;

    /**
     * 是否接近配额上限（超过90%认为接近）
     */
    public boolean isNearQuota() {
        return usedPercentage != null && usedPercentage > 90.0;
    }

    /**
     * 是否达到配额上限
     */
    public boolean isQuotaExceeded() {
        return usedPercentage != null && usedPercentage >= 100.0;
    }

    /**
     * 获取剩余空间
     */
    public long getRemainingBytes() {
        if (totalBytes <= 0) return Long.MAX_VALUE;
        return totalBytes - usedBytes;
    }
}
