package com.basebackend.storage.spi;

import com.basebackend.storage.model.StorageResult;
import com.basebackend.storage.model.StorageUsage;
import com.basebackend.storage.model.UploadRequest;

import java.io.InputStream;
import java.util.List;

/**
 * 统一存储服务接口
 * <p>
 * 支持多种存储后端：本地文件系统、MinIO、阿里云OSS、AWS S3
 * 
 * @author BaseBackend
 */
public interface StorageProvider {
    
    /**
     * 上传文件
     *
     * @param request 上传请求
     * @return 存储结果
     */
    StorageResult upload(UploadRequest request);
    
    /**
     * 下载文件
     *
     * @param bucket 存储桶
     * @param key    对象键名
     * @return 文件输入流
     */
    InputStream download(String bucket, String key);
    
    /**
     * 下载文件（使用默认桶）
     *
     * @param key 对象键名
     * @return 文件输入流
     */
    default InputStream download(String key) {
        return download(getDefaultBucket(), key);
    }
    
    /**
     * 删除文件
     *
     * @param bucket 存储桶
     * @param key    对象键名
     */
    void delete(String bucket, String key);
    
    /**
     * 删除文件（使用默认桶）
     *
     * @param key 对象键名
     */
    default void delete(String key) {
        delete(getDefaultBucket(), key);
    }
    
    /**
     * 复制文件
     *
     * @param sourceBucket 源存储桶
     * @param sourceKey    源键名
     * @param targetBucket 目标存储桶
     * @param targetKey    目标键名
     */
    void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey);
    
    /**
     * 复制文件（同桶内）
     *
     * @param sourceKey 源键名
     * @param targetKey 目标键名
     */
    default void copy(String sourceKey, String targetKey) {
        copy(getDefaultBucket(), sourceKey, getDefaultBucket(), targetKey);
    }
    
    /**
     * 移动文件
     *
     * @param sourceBucket 源存储桶
     * @param sourceKey    源键名
     * @param targetBucket 目标存储桶
     * @param targetKey    目标键名
     */
    default void move(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        copy(sourceBucket, sourceKey, targetBucket, targetKey);
        delete(sourceBucket, sourceKey);
    }
    
    /**
     * 移动文件（同桶内）
     *
     * @param sourceKey 源键名
     * @param targetKey 目标键名
     */
    default void move(String sourceKey, String targetKey) {
        move(getDefaultBucket(), sourceKey, getDefaultBucket(), targetKey);
    }
    
    /**
     * 检查文件是否存在
     *
     * @param bucket 存储桶
     * @param key    对象键名
     * @return 是否存在
     */
    boolean exists(String bucket, String key);
    
    /**
     * 检查文件是否存在（使用默认桶）
     *
     * @param key 对象键名
     * @return 是否存在
     */
    default boolean exists(String key) {
        return exists(getDefaultBucket(), key);
    }
    
    /**
     * 获取文件访问URL
     *
     * @param bucket 存储桶
     * @param key    对象键名
     * @return 访问URL
     */
    String getUrl(String bucket, String key);
    
    /**
     * 获取文件访问URL（使用默认桶）
     *
     * @param key 对象键名
     * @return 访问URL
     */
    default String getUrl(String key) {
        return getUrl(getDefaultBucket(), key);
    }
    
    /**
     * 获取预签名URL（临时访问）
     *
     * @param bucket        存储桶
     * @param key           对象键名
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    String getPresignedUrl(String bucket, String key, int expireSeconds);
    
    /**
     * 获取预签名URL（使用默认桶）
     *
     * @param key           对象键名
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    default String getPresignedUrl(String key, int expireSeconds) {
        return getPresignedUrl(getDefaultBucket(), key, expireSeconds);
    }
    
    /**
     * 列出对象
     *
     * @param bucket 存储桶
     * @param prefix 前缀过滤
     * @return 对象键名列表
     */
    List<String> listObjects(String bucket, String prefix);
    
    /**
     * 列出对象（使用默认桶）
     *
     * @param prefix 前缀过滤
     * @return 对象键名列表
     */
    default List<String> listObjects(String prefix) {
        return listObjects(getDefaultBucket(), prefix);
    }
    
    /**
     * 获取存储使用量
     *
     * @param bucket 存储桶
     * @return 使用量统计
     */
    StorageUsage getUsage(String bucket);
    
    /**
     * 获取存储使用量（使用默认桶）
     *
     * @return 使用量统计
     */
    default StorageUsage getUsage() {
        return getUsage(getDefaultBucket());
    }
    
    /**
     * 获取存储类型
     *
     * @return 存储类型
     */
    StorageType getStorageType();
    
    /**
     * 获取默认存储桶
     *
     * @return 默认桶名称
     */
    String getDefaultBucket();
    
    /**
     * 验证文件完整性（可选实现）
     *
     * @param bucket         存储桶
     * @param key            对象键名
     * @param expectedMd5    期望的MD5
     * @param expectedSha256 期望的SHA256
     * @return 验证是否通过
     */
    default boolean verify(String bucket, String key, String expectedMd5, String expectedSha256) {
        // 默认不支持，子类可覆盖
        return true;
    }
    
    /**
     * 获取支持的功能特性
     *
     * @return 特性列表，如 ["multipart_upload", "versioning", "encryption"]
     */
    default String[] getSupportedFeatures() {
        return new String[0];
    }
}
