package com.basebackend.file.config;

import com.basebackend.file.storage.StorageService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一文件存储配置属性
 *
 * 整合所有存储类型配置，消除硬编码凭证和配置分散问题
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 默认存储类型
     */
    private StorageService.StorageType type = StorageService.StorageType.LOCAL;

    /**
     * 默认存储桶名称
     */
    private String defaultBucket = "basebackend-files";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * MinIO配置
     */
    private MinioConfig minio = new MinioConfig();

    /**
     * 阿里云OSS配置
     */
    private OssConfig oss = new OssConfig();

    /**
     * AWS S3配置
     */
    private S3Config s3 = new S3Config();

    /**
     * 本地存储配置
     */
    @Data
    public static class Local {
        /**
         * 是否启用本地存储
         */
        private boolean enabled = true;

        /**
         * 上传路径
         */
        private String uploadPath = "./uploads";

        /**
         * 访问前缀
         */
        private String accessPrefix = "/files";

        /**
         * 最大文件大小（字节）默认10MB
         */
        private long maxSize = 10 * 1024 * 1024;
    }

    /**
     * MinIO配置
     */
    @Data
    public static class MinioConfig {
        /**
         * 是否启用MinIO
         */
        private boolean enabled = false;

        /**
         * 服务地址
         */
        private String endpoint;

        /**
         * Access Key
         */
        private String accessKey;

        /**
         * Secret Key
         */
        private String secretKey;

        /**
         * 存储桶名称
         */
        private String bucketName;

        /**
         * 区域（可选）
         */
        private String region;

        /**
         * 路径前缀
         */
        private String pathPrefix = "files";

        /**
         * 是否使用SSL
         */
        private boolean secure = false;
    }

    /**
     * 阿里云OSS配置
     */
    @Data
    public static class OssConfig {
        /**
         * 是否启用OSS
         */
        private boolean enabled = false;

        /**
         * 服务端点
         */
        private String endpoint;

        /**
         * AccessKey ID
         */
        private String accessKeyId;

        /**
         * AccessKey Secret
         */
        private String accessKeySecret;

        /**
         * 存储桶名称
         */
        private String bucketName;

        /**
         * 区域
         */
        private String region;

        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }

    /**
     * AWS S3配置
     */
    @Data
    public static class S3Config {
        /**
         * 是否启用S3
         */
        private boolean enabled = false;

        /**
         * 服务端点
         */
        private String endpoint;

        /**
         * Access Key ID
         */
        private String accessKeyId;

        /**
         * Secret Access Key
         */
        private String secretAccessKey;

        /**
         * 存储桶名称
         */
        private String bucketName;

        /**
         * 区域
         */
        private String region;

        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }
}
