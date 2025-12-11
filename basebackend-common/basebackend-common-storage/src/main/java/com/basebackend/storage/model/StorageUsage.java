package com.basebackend.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储使用量统计
 * 
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsage {
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * 已使用空间（字节）
     */
    private Long usedBytes;
    
    /**
     * 总容量（字节），-1表示无限制
     */
    @Builder.Default
    private Long totalBytes = -1L;
    
    /**
     * 文件数量
     */
    private Long fileCount;
    
    /**
     * 统计时间
     */
    private LocalDateTime calculatedAt;
    
    /**
     * 使用百分比（0-100）
     */
    public double getUsagePercentage() {
        if (totalBytes == null || totalBytes <= 0) {
            return 0.0;
        }
        return (usedBytes * 100.0) / totalBytes;
    }
    
    /**
     * 格式化已使用空间
     */
    public String getFormattedUsedSpace() {
        return formatBytes(usedBytes);
    }
    
    /**
     * 格式化总容量
     */
    public String getFormattedTotalSpace() {
        if (totalBytes == null || totalBytes < 0) {
            return "无限制";
        }
        return formatBytes(totalBytes);
    }
    
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "N/A";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.2f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        }
        double gb = mb / 1024.0;
        if (gb < 1024) {
            return String.format("%.2f GB", gb);
        }
        double tb = gb / 1024.0;
        return String.format("%.2f TB", tb);
    }
}
