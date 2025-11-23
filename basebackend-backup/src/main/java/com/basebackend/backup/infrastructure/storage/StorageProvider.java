package com.basebackend.backup.infrastructure.storage;

import java.io.InputStream;
import java.util.List;

/**
 * 存储提供者接口
 * 统一抽象不同存储后端（本地文件系统、S3、OSS、MinIO等）
 */
public interface StorageProvider {

    /**
     * 上传文件
     *
     * @param request 上传请求
     * @return 存储结果
     * @throws Exception 上传失败时抛出异常
     */
    StorageResult upload(UploadRequest request) throws Exception;

    /**
     * 下载文件
     *
     * @param bucket 存储桶
     * @param key 键名
     * @return 文件输入流
     * @throws Exception 下载失败时抛出异常
     */
    InputStream download(String bucket, String key) throws Exception;

    /**
     * 删除文件
     *
     * @param bucket 存储桶
     * @param key 键名
     * @return 删除是否成功
     * @throws Exception 删除失败时抛出异常
     */
    boolean delete(String bucket, String key) throws Exception;

    /**
     * 验证文件是否存在
     *
     * @param bucket 存储桶
     * @param key 键名
     * @return 是否存在
     * @throws Exception 检查失败时抛出异常
     */
    boolean exists(String bucket, String key) throws Exception;

    /**
     * 验证文件完整性
     *
     * @param bucket 存储桶
     * @param key 键名
     * @param expectedMd5 期望的MD5校验和
     * @param expectedSha256 期望的SHA256校验和
     * @return 验证是否通过
     * @throws Exception 验证失败时抛出异常
     */
    boolean verify(String bucket, String key, String expectedMd5, String expectedSha256) throws Exception;

    /**
     * 获取存储使用量统计
     *
     * @param bucket 存储桶（可选，某些存储系统可能不支持按桶统计）
     * @return 使用量信息
     * @throws Exception 获取失败时抛出异常
     */
    StorageUsage getUsage(String bucket) throws Exception;

    /**
     * 获取支持的功能特性
     *
     * @return 支持的特性列表，如["multipart_upload", "object_versioning", "encryption"]
     */
    String[] getSupportedFeatures();

    /**
     * 获取存储类型
     *
     * @return 存储类型标识符，如"local"、"s3"、"oss"、"minio"
     */
    String getStorageType();

    /**
     * 获取访问URL（如果支持）
     *
     * @param bucket 存储桶
     * @param key 键名
     * @param expirationMinutes 过期时间（分钟）
     * @return 预签名URL
     * @throws Exception 生成失败时抛出异常
     */
    String getPresignedUrl(String bucket, String key, int expirationMinutes) throws Exception;
}
