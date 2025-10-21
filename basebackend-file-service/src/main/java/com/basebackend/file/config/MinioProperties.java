package com.basebackend.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO配置属性
 *
 * @author BaseBackend
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * 是否启用MinIO
     */
    private boolean enabled = true;

    /**
     * MinIO服务地址
     */
    private String endpoint = "http://localhost:9000";

    /**
     * Access Key
     */
    private String accessKey = "minioadmin";

    /**
     * Secret Key
     */
    private String secretKey = "minioadmin";

    /**
     * 默认存储桶名称
     */
    private String bucketName = "basebackend";

    /**
     * 文件上传路径前缀
     */
    private String pathPrefix = "files";

    /**
     * 图片上传路径前缀
     */
    private String imagePath = "images";

    /**
     * 大文件上传路径前缀
     */
    private String largePath = "large";

    /**
     * 缩略图路径前缀
     */
    private String thumbnailPath = "thumbnails";

    /**
     * 单个文件最大大小（MB）
     */
    private long maxFileSize = 100;

    /**
     * 大文件阈值（MB），超过此大小使用分片上传
     */
    private long largeFileThreshold = 10;

    /**
     * 分片大小（MB）
     */
    private long partSize = 5;

    /**
     * 缩略图宽度
     */
    private int thumbnailWidth = 200;

    /**
     * 缩略图高度
     */
    private int thumbnailHeight = 200;

    /**
     * 图片压缩质量（0.0-1.0）
     */
    private float imageQuality = 0.8f;
}
