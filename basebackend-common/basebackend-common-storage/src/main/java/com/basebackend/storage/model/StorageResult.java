package com.basebackend.storage.model;

import com.basebackend.storage.spi.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 存储操作结果对象
 * 
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageResult {
    
    /**
     * 存储位置（完整URI或路径）
     */
    private String location;
    
    /**
     * 存储键名
     */
    private String key;
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * ETag（对象版本标识）
     */
    private String etag;
    
    /**
     * 版本ID（支持版本控制的存储系统）
     */
    private String versionId;
    
    /**
     * 文件大小（字节）
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
     * 存储类型
     */
    private StorageType storageType;
    
    /**
     * 存储区域（云存储场景）
     */
    private String region;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 访问URL（如果可用）
     */
    private String accessUrl;
    
    /**
     * 操作是否成功
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 创建成功结果
     */
    public static StorageResult success(String bucket, String key, String location) {
        return StorageResult.builder()
                .bucket(bucket)
                .key(key)
                .location(location)
                .success(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static StorageResult failure(String errorMessage) {
        return StorageResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
