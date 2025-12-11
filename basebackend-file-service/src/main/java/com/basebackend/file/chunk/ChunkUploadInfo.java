package com.basebackend.file.chunk;

import lombok.Data;

/**
 * 分块上传信息
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class ChunkUploadInfo {

    /**
     * 上传ID
     */
    private String uploadId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 分块大小（字节）
     */
    private int chunkSize;

    /**
     * 总分块数
     */
    private int totalChunks;

    /**
     * 已上传的分块数
     */
    private int uploadedChunks;

    /**
     * 文件MD5（用于校验）
     */
    private String fileMd5;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 目标存储路径
     */
    private String targetPath;

    /**
     * 创建时间戳
     */
    private long createTime;

    /**
     * 最后更新时间戳
     */
    private long lastUpdateTime;

    /**
     * 上传状态
     */
    private UploadStatus status;

    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        /** 初始化 */
        INITIALIZED,
        /** 上传中 */
        UPLOADING,
        /** 已完成 */
        COMPLETED,
        /** 已失败 */
        FAILED,
        /** 已取消 */
        CANCELLED,
        /** 已过期 */
        EXPIRED
    }

    /**
     * 检查是否可以继续上传
     */
    public boolean canContinue() {
        return status == UploadStatus.INITIALIZED || status == UploadStatus.UPLOADING;
    }

    /**
     * 计算上传进度（百分比）
     */
    public int getProgress() {
        if (totalChunks == 0) {
            return 0;
        }
        return (int) ((uploadedChunks * 100L) / totalChunks);
    }

    /**
     * 判断是否所有分块都已上传
     */
    public boolean isAllChunksUploaded() {
        return uploadedChunks >= totalChunks;
    }
}
