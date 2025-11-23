package com.basebackend.backup.infrastructure.storage;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 存储操作结果对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageResult {
    /**
     * 存储位置（URI或路径）
     */
    private String location;

    /**
     * 存储键名
     */
    private String key;

    /**
     * 存储桶名称（云存储场景）
     */
    private String bucket;

    /**
     * ETag（对象版本标识）
     */
    private String etag;

    /**
     * 版本ID（某些存储系统支持版本控制）
     */
    private String versionId;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;

    /**
     * 存储类型：local、s3、oss、minio等
     */
    private String storageType;

    /**
     * 存储区域（针对云存储）
     */
    private String region;

    /**
     * 额外信息
     */
    private Map<String, Object> metadata;

    /**
     * 访问URL（如果支持）
     */
    private String accessUrl;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
}
