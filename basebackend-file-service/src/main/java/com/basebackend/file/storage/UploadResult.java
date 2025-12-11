package com.basebackend.file.storage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传结果
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Data
@Builder(builderClassName = "Builder", builderMethodName = "builder", toBuilder = true)
public class UploadResult {

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private long size;

    /**
     * 文件MD5校验和
     */
    private String md5Hash;

    /**
     * 缩略图URL（如果有）
     */
    private String thumbnailUrl;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 存储类型
     */
    private StorageService.StorageType storageType;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 错误码（便于定位问题）
     */
    private String errorCode;

    /**
     * 错误详情（如果有）
     */
    private Object errorDetail;

    /**
     * 创建成功结果
     */
    public static UploadResult success(String url, String path, String bucket,
                                      String contentType, long size,
                                      StorageService.StorageType storageType) {
        return UploadResult.builder()
                .url(url)
                .path(path)
                .bucket(bucket)
                .contentType(contentType)
                .size(size)
                .storageType(storageType)
                .uploadTime(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 创建成功结果（完整版）
     */
    public static UploadResult success(String url, String path, String bucket,
                                      String contentType, long size,
                                      String md5Hash, String thumbnailUrl,
                                      StorageService.StorageType storageType) {
        return UploadResult.builder()
                .url(url)
                .path(path)
                .bucket(bucket)
                .contentType(contentType)
                .size(size)
                .md5Hash(md5Hash)
                .thumbnailUrl(thumbnailUrl)
                .storageType(storageType)
                .uploadTime(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static UploadResult failure(String errorMessage) {
        return UploadResult.builder()
                .errorMessage(errorMessage)
                .success(false)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败结果（带错误码）
     */
    public static UploadResult failure(String errorMessage, String errorCode) {
        return UploadResult.builder()
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .success(false)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败结果（完整版）
     */
    public static UploadResult failure(String errorMessage, String errorCode, Object errorDetail) {
        return UploadResult.builder()
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .errorDetail(errorDetail)
                .success(false)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    /**
     * 检查是否为成功结果
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 检查是否为失败结果
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * 获取错误信息（如果失败）
     */
    public String getErrorMessage() {
        return success ? null : errorMessage;
    }

    /**
     * 获取错误码（如果失败）
     */
    public String getErrorCode() {
        return success ? null : errorCode;
    }

    /**
     * 验证结果完整性
     */
    public void validate() {
        if (success) {
            // 成功结果的必要字段
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalStateException("成功结果中url不能为空");
            }
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalStateException("成功结果中path不能为空");
            }
            if (storageType == null) {
                throw new IllegalStateException("成功结果中storageType不能为空");
            }
            if (size < 0) {
                throw new IllegalStateException("成功结果中size不能为负数");
            }
        } else {
            // 失败结果的必要字段
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalStateException("失败结果中errorMessage不能为空");
            }
        }

        // 通用验证
        if (uploadTime == null) {
            throw new IllegalStateException("uploadTime不能为null");
        }
    }

    @Override
    public String toString() {
        if (success) {
            return String.format(
                    "UploadResult{success=true, url='%s', path='%s', bucket='%s', type=%s, size=%d}",
                    url, path, bucket, storageType, size);
        } else {
            return String.format(
                    "UploadResult{success=false, error='%s', code='%s'}",
                    errorMessage, errorCode);
        }
    }
}
