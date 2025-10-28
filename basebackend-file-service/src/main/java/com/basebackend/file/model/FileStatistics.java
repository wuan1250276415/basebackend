package com.basebackend.file.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件统计信息
 */
@Data
public class FileStatistics {

    /**
     * 文件总数
     */
    private long totalFiles;

    /**
     * 文件总大小
     */
    private long totalSize;

    /**
     * 存储使用情况
     */
    private StorageUsage storageUsage = new StorageUsage();

    /**
     * 文件类型分布
     */
    private List<FileTypeDistribution> fileTypeDistribution = new ArrayList<>();

    @Data
    public static class StorageUsage {
        private long local;
        private long minio;
        private long oss;
        private long s3;
    }

    @Data
    public static class FileTypeDistribution {
        private String type;
        private long count;
        private long size;
    }
}
