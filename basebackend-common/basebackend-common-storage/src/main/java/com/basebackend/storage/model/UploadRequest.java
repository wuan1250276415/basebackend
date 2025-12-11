package com.basebackend.storage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.Map;

/**
 * 文件上传请求对象
 * 
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {
    
    /**
     * 存储桶/容器名称
     */
    private String bucket;
    
    /**
     * 对象键名（路径）
     */
    private String key;
    
    /**
     * 文件输入流
     */
    private InputStream inputStream;
    
    /**
     * 文件大小（字节）
     */
    private Long size;
    
    /**
     * 内容类型（MIME类型）
     */
    private String contentType;
    
    /**
     * MD5校验和（可选，用于完整性验证）
     */
    private String md5Checksum;
    
    /**
     * SHA256校验和（可选，用于完整性验证）
     */
    private String sha256Checksum;
    
    /**
     * 自定义元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 是否使用分块上传
     */
    @Builder.Default
    private boolean multipart = false;
    
    /**
     * 分块大小（字节），默认16MB
     */
    @Builder.Default
    private long chunkSize = 16L * 1024 * 1024;
    
    /**
     * 简化构造方法：用于简单上传场景
     */
    public static UploadRequest of(String bucket, String key, InputStream inputStream, long size, String contentType) {
        return UploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .inputStream(inputStream)
                .size(size)
                .contentType(contentType)
                .build();
    }
    
    /**
     * 简化构造方法：使用默认桶
     */
    public static UploadRequest of(String key, InputStream inputStream, long size, String contentType) {
        return UploadRequest.builder()
                .key(key)
                .inputStream(inputStream)
                .size(size)
                .contentType(contentType)
                .build();
    }
}
