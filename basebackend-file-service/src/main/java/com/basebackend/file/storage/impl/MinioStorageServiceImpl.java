package com.basebackend.file.storage.impl;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.MinioProperties;
import com.basebackend.file.storage.StorageService;
import com.basebackend.file.storage.StorageServiceRegistry;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO存储实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "minio")
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final StorageServiceRegistry storageServiceRegistry;

    /**
     * 初始化时自动注册到存储服务注册中心
     */
    @PostConstruct
    public void init() {
        storageServiceRegistry.registerService(StorageType.MINIO, this);
        log.info("MinIO存储服务已注册到存储服务注册中心");
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long size) {
        try {
            // 确保bucket存在
            ensureBucketExists();

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());

            log.info("MinIO存储：文件上传成功 bucket={}, path={}", minioProperties.getBucketName(), path);
            return getUrl(path);
        } catch (Exception e) {
            log.error("MinIO存储：文件上传失败 path={}", path, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .build());
        } catch (Exception e) {
            log.error("MinIO存储：文件下载失败 path={}", path, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .build());
            log.info("MinIO存储：文件删除成功 path={}", path);
        } catch (Exception e) {
            log.error("MinIO存储：文件删除失败 path={}", path, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public void copy(String sourcePath, String targetPath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(targetPath)
                            .source(
                                    CopySource.builder()
                                            .bucket(minioProperties.getBucketName())
                                            .object(sourcePath)
                                            .build())
                            .build());
            log.info("MinIO存储：文件复制成功 from={} to={}", sourcePath, targetPath);
        } catch (Exception e) {
            log.error("MinIO存储：文件复制失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件复制失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        // MinIO不支持直接移动，需要先复制再删除
        copy(sourcePath, targetPath);
        delete(sourcePath);
    }

    @Override
    public boolean exists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getUrl(String path) {
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + path;
    }

    @Override
    public String getPresignedUrl(String path, int expireTime) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .expiry(expireTime, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("MinIO存储：获取签名URL失败 path={}", path, e);
            throw new BusinessException("获取签名URL失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listFiles(String prefix) {
        List<String> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    files.add(item.objectName());
                }
            }
        } catch (Exception e) {
            log.error("MinIO存储：列出文件失败 prefix={}", prefix, e);
            throw new BusinessException("列出文件失败: " + e.getMessage());
        }
        return files;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.MINIO;
    }

    /**
     * 确保bucket存在
     */
    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build());

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .build());
                log.info("MinIO存储：创建bucket成功 bucket={}", minioProperties.getBucketName());
            }
        } catch (Exception e) {
            log.error("MinIO存储：检查bucket失败", e);
            throw new BusinessException("检查bucket失败: " + e.getMessage());
        }
    }
}
