package com.basebackend.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件上传结果
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 存储文件名
     */
    private String storedFilename;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME）
     */
    private String contentType;

    /**
     * 缩略图URL（仅图片）
     */
    private String thumbnailUrl;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * ETag（用于验证）
     */
    private String etag;
}
