package com.basebackend.backup.infrastructure.storage;

import lombok.Data;

import java.io.InputStream;
import java.util.Map;

/**
 * 文件上传请求对象
 */
@Data
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
     * 内容类型
     */
    private String contentType;

    /**
     * MD5校验和（可选）
     */
    private String md5Checksum;

    /**
     * 自定义元数据
     */
    private Map<String, String> metadata;

    /**
     * 是否分块上传
     */
    private boolean multipart = false;

    /**
     * 分块大小（字节）
     */
    private Long chunkSize = 16L * 1024 * 1024; // 默认16MB
}
