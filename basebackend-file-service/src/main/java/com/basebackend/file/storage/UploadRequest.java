package com.basebackend.file.storage;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.io.InputStream;
import java.util.Map;

/**
 * 文件上传请求
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Data
public class UploadRequest {

    /**
     * 文件输入流
     */
    @NotNull(message = "文件输入流不能为null")
    private InputStream inputStream;

    /**
     * 文件路径（相对于bucket的路径）
     */
    @NotNull(message = "文件路径不能为null")
    @Size(min = 1, max = 1024, message = "文件路径长度必须在1-1024字符之间")
    private String path;

    /**
     * 存储桶名称
     */
    @Size(max = 255, message = "存储桶名称长度不能超过255字符")
    private String bucket;

    /**
     * 文件内容类型
     */
    @Size(max = 255, message = "文件内容类型长度不能超过255字符")
    private String contentType;

    /**
     * 文件大小（字节）
     */
    @Min(value = 0, message = "文件大小不能为负数")
    private long size;

    /**
     * 存储类型
     */
    @NotNull(message = "存储类型不能为null")
    private StorageService.StorageType storageType;

    /**
     * 是否生成缩略图（仅图片文件）
     */
    private boolean generateThumbnail = false;

    /**
     * 缩略图宽度
     */
    @Min(value = 1, message = "缩略图宽度必须大于0")
    @Max(value = 10000, message = "缩略图宽度不能超过10000")
    private int thumbnailWidth = 200;

    /**
     * 缩略图高度
     */
    @Min(value = 1, message = "缩略图高度必须大于0")
    @Max(value = 10000, message = "缩略图高度不能超过10000")
    private int thumbnailHeight = 200;

    /**
     * 允许的最大大小（字节）
     */
    @Min(value = 1, message = "最大大小必须大于0")
    private long maxSize = Long.MAX_VALUE;

    /**
     * 允许的文件类型
     */
    private String[] allowedTypes;

    /**
     * 文件元数据（使用Map提高类型安全性）
     */
    private Map<String, Object> metadata;

    /**
     * 验证请求参数
     */
    public void validate() {
        // 验证必要字段
        if (inputStream == null) {
            throw new IllegalArgumentException("文件输入流不能为null");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (storageType == null) {
            throw new IllegalArgumentException("存储类型不能为null");
        }

        // 验证路径格式
        if (path.contains("..") || path.startsWith("/") || path.endsWith("/")) {
            throw new IllegalArgumentException("文件路径格式不正确");
        }

        // 验证大小
        if (size < 0) {
            throw new IllegalArgumentException("文件大小不能为负数");
        }
        if (size > maxSize) {
            throw new IllegalArgumentException(
                    String.format("文件大小 %d 字节超过限制 %d 字节", size, maxSize));
        }

        // 验证缩略图参数
        if (thumbnailWidth <= 0 || thumbnailWidth > 10000) {
            throw new IllegalArgumentException("缩略图宽度必须在1-10000之间");
        }
        if (thumbnailHeight <= 0 || thumbnailHeight > 10000) {
            throw new IllegalArgumentException("缩略图高度必须在1-10000之间");
        }

        // 验证文件类型（如果指定了允许类型列表）
        if (allowedTypes != null && allowedTypes.length > 0 && contentType != null) {
            boolean allowed = false;
            for (String type : allowedTypes) {
                if (type != null && (contentType.equals(type) || contentType.startsWith(type + "/"))) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new IllegalArgumentException("文件类型 " + contentType + " 不被允许");
            }
        }
    }

    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private final UploadRequest request = new UploadRequest();

        public Builder inputStream(InputStream inputStream) {
            request.inputStream = inputStream;
            return this;
        }

        public Builder path(String path) {
            request.path = path;
            return this;
        }

        public Builder bucket(String bucket) {
            request.bucket = bucket;
            return this;
        }

        public Builder contentType(String contentType) {
            request.contentType = contentType;
            return this;
        }

        public Builder size(long size) {
            request.size = size;
            return this;
        }

        public Builder storageType(StorageService.StorageType storageType) {
            request.storageType = storageType;
            return this;
        }

        public Builder generateThumbnail(boolean generateThumbnail) {
            request.generateThumbnail = generateThumbnail;
            return this;
        }

        public Builder thumbnailWidth(int thumbnailWidth) {
            request.thumbnailWidth = thumbnailWidth;
            return this;
        }

        public Builder thumbnailHeight(int thumbnailHeight) {
            request.thumbnailHeight = thumbnailHeight;
            return this;
        }

        public Builder maxSize(long maxSize) {
            request.maxSize = maxSize;
            return this;
        }

        public Builder allowedTypes(String[] allowedTypes) {
            request.allowedTypes = allowedTypes;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            request.metadata = metadata;
            return this;
        }

        public UploadRequest build() {
            // 在build时进行验证
            request.validate();
            return request;
        }
    }
}
