package com.basebackend.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储配置属性
 * 
 * @author BaseBackend
 */
@Data
@ConfigurationProperties(prefix = "basebackend.storage")
public class StorageProperties {
    
    /**
     * 默认存储类型：local, minio, oss, s3
     */
    private String type = "local";
    
    /**
     * 默认存储桶
     */
    private String defaultBucket = "default";
    
    /**
     * 本地存储配置
     */
    private Local local = new Local();
    
    /**
     * MinIO 配置
     */
    private Minio minio = new Minio();
    
    /**
     * 阿里云 OSS 配置
     */
    private Oss oss = new Oss();
    
    /**
     * AWS S3 配置
     */
    private S3 s3 = new S3();
    
    /**
     * 校验配置
     */
    private ChecksumConfig checksum = new ChecksumConfig();
    
    /**
     * 本地存储配置
     */
    @Data
    public static class Local {
        /**
         * 存储根目录
         */
        private String basePath = "./storage";
        
        /**
         * 访问URL前缀
         */
        private String urlPrefix = "/files";
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
    }
    
    /**
     * MinIO 配置
     */
    @Data
    public static class Minio {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 服务端点
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
         * 默认桶
         */
        private String bucket = "default";
        
        /**
         * 是否使用 HTTPS
         */
        private boolean secure = false;
    }
    
    /**
     * 阿里云 OSS 配置
     */
    @Data
    public static class Oss {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 端点
         */
        private String endpoint;
        
        /**
         * Access Key ID
         */
        private String accessKeyId;
        
        /**
         * Access Key Secret
         */
        private String accessKeySecret;
        
        /**
         * 默认桶
         */
        private String bucket;
        
        /**
         * 自定义域名（可选）
         */
        private String customDomain;
    }
    
    /**
     * AWS S3 配置
     */
    @Data
    public static class S3 {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 区域
         */
        private String region = "us-east-1";
        
        /**
         * Access Key ID
         */
        private String accessKeyId;
        
        /**
         * Secret Access Key
         */
        private String secretAccessKey;
        
        /**
         * 默认桶
         */
        private String bucket;
        
        /**
         * 端点（用于兼容S3的服务，如MinIO）
         */
        private String endpoint;
        
        /**
         * 是否使用路径样式访问
         */
        private boolean pathStyleAccessEnabled = false;
    }
    
    /**
     * 校验配置
     */
    @Data
    public static class ChecksumConfig {
        /**
         * 是否启用校验
         */
        private boolean enabled = true;
        
        /**
         * 算法列表
         */
        private String[] algorithms = {"MD5", "SHA256"};
    }
}
