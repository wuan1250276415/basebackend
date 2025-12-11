package com.basebackend.file.dedup;

import lombok.Data;

/**
 * 文件去重信息
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class DeduplicationInfo {

    /**
     * 文件内容哈希值（SHA-256）
     */
    private String contentHash;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 实际存储路径
     */
    private String storagePath;

    /**
     * 引用计数
     */
    private int referenceCount;

    /**
     * 首次上传时间
     */
    private long firstUploadTime;

    /**
     * 最后引用时间
     */
    private long lastReferenceTime;

    /**
     * 是否为重复文件
     */
    private boolean duplicate;

    /**
     * 创建非重复文件信息
     */
    public static DeduplicationInfo newFile(String contentHash, long fileSize, String storagePath) {
        DeduplicationInfo info = new DeduplicationInfo();
        info.setContentHash(contentHash);
        info.setFileSize(fileSize);
        info.setStoragePath(storagePath);
        info.setReferenceCount(1);
        info.setFirstUploadTime(System.currentTimeMillis());
        info.setLastReferenceTime(System.currentTimeMillis());
        info.setDuplicate(false);
        return info;
    }

    /**
     * 创建重复文件信息（引用已存在的文件）
     */
    public static DeduplicationInfo duplicateFile(DeduplicationInfo existing) {
        DeduplicationInfo info = new DeduplicationInfo();
        info.setContentHash(existing.getContentHash());
        info.setFileSize(existing.getFileSize());
        info.setStoragePath(existing.getStoragePath());
        info.setReferenceCount(existing.getReferenceCount() + 1);
        info.setFirstUploadTime(existing.getFirstUploadTime());
        info.setLastReferenceTime(System.currentTimeMillis());
        info.setDuplicate(true);
        return info;
    }
}
