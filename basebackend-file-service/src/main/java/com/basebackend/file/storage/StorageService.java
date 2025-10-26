package com.basebackend.file.storage;

import java.io.InputStream;
import java.util.List;

/**
 * 存储服务接口
 * 支持多种存储后端：本地、MinIO、阿里云OSS、AWS S3
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param path        存储路径
     * @param contentType 文件类型
     * @param size        文件大小
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String path, String contentType, long size);

    /**
     * 下载文件
     *
     * @param path 存储路径
     * @return 文件输入流
     */
    InputStream download(String path);

    /**
     * 删除文件
     *
     * @param path 存储路径
     */
    void delete(String path);

    /**
     * 复制文件
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     */
    void copy(String sourcePath, String targetPath);

    /**
     * 移动文件
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     */
    void move(String sourcePath, String targetPath);

    /**
     * 文件是否存在
     *
     * @param path 存储路径
     * @return 是否存在
     */
    boolean exists(String path);

    /**
     * 获取文件URL
     *
     * @param path 存储路径
     * @return 访问URL
     */
    String getUrl(String path);

    /**
     * 获取临时访问URL（带签名）
     *
     * @param path       存储路径
     * @param expireTime 过期时间（秒）
     * @return 临时访问URL
     */
    String getPresignedUrl(String path, int expireTime);

    /**
     * 列出目录下的文件
     *
     * @param prefix 路径前缀
     * @return 文件路径列表
     */
    List<String> listFiles(String prefix);

    /**
     * 获取存储类型
     *
     * @return 存储类型
     */
    StorageType getStorageType();

    /**
     * 存储类型枚举
     */
    enum StorageType {
        LOCAL("本地存储"),
        MINIO("MinIO对象存储"),
        ALIYUN_OSS("阿里云OSS"),
        AWS_S3("AWS S3");

        private final String description;

        StorageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
