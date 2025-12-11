package com.basebackend.storage.spi;

/**
 * 存储类型枚举
 * 
 * @author BaseBackend
 */
public enum StorageType {
    
    /**
     * 本地文件系统存储
     */
    LOCAL("local", "本地存储"),
    
    /**
     * MinIO 对象存储
     */
    MINIO("minio", "MinIO对象存储"),
    
    /**
     * 阿里云 OSS
     */
    ALIYUN_OSS("oss", "阿里云OSS"),
    
    /**
     * AWS S3
     */
    AWS_S3("s3", "AWS S3");
    
    private final String code;
    private final String description;
    
    StorageType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取存储类型
     */
    public static StorageType fromCode(String code) {
        for (StorageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的存储类型: " + code);
    }
}
