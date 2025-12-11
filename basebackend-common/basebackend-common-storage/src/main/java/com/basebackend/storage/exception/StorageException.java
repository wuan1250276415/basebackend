package com.basebackend.storage.exception;

/**
 * 存储操作异常
 * 
 * @author BaseBackend
 */
public class StorageException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误代码
     */
    private final String errorCode;
    
    /**
     * 存储类型
     */
    private final String storageType;
    
    public StorageException(String message) {
        super(message);
        this.errorCode = "STORAGE_ERROR";
        this.storageType = null;
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "STORAGE_ERROR";
        this.storageType = null;
    }
    
    public StorageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.storageType = null;
    }
    
    public StorageException(String errorCode, String message, String storageType) {
        super(message);
        this.errorCode = errorCode;
        this.storageType = storageType;
    }
    
    public StorageException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.storageType = null;
    }
    
    public StorageException(String errorCode, String message, String storageType, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.storageType = storageType;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getStorageType() {
        return storageType;
    }
    
    // ===== 常见异常工厂方法 =====
    
    public static StorageException uploadFailed(String path, Throwable cause) {
        return new StorageException("UPLOAD_FAILED", "文件上传失败: " + path, cause);
    }
    
    public static StorageException downloadFailed(String path, Throwable cause) {
        return new StorageException("DOWNLOAD_FAILED", "文件下载失败: " + path, cause);
    }
    
    public static StorageException deleteFailed(String path, Throwable cause) {
        return new StorageException("DELETE_FAILED", "文件删除失败: " + path, cause);
    }
    
    public static StorageException fileNotFound(String path) {
        return new StorageException("FILE_NOT_FOUND", "文件不存在: " + path);
    }
    
    public static StorageException bucketNotFound(String bucket) {
        return new StorageException("BUCKET_NOT_FOUND", "存储桶不存在: " + bucket);
    }
    
    public static StorageException accessDenied(String path) {
        return new StorageException("ACCESS_DENIED", "访问被拒绝: " + path);
    }
    
    public static StorageException configurationError(String message) {
        return new StorageException("CONFIG_ERROR", "存储配置错误: " + message);
    }
}
