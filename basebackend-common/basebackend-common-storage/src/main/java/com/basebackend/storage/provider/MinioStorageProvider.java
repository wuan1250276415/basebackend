package com.basebackend.storage.provider;

import com.basebackend.storage.config.StorageProperties;
import com.basebackend.storage.exception.StorageException;
import com.basebackend.storage.model.StorageResult;
import com.basebackend.storage.model.StorageUsage;
import com.basebackend.storage.model.UploadRequest;
import com.basebackend.storage.spi.StorageProvider;
import com.basebackend.storage.spi.StorageType;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MinIO 对象存储实现
 * 
 * @author BaseBackend
 */
@Slf4j
public class MinioStorageProvider implements StorageProvider {
    
    private final MinioClient minioClient;
    private final StorageProperties.Minio minioConfig;
    private final String defaultBucket;
    
    public MinioStorageProvider(StorageProperties properties) {
        this.minioConfig = properties.getMinio();
        this.defaultBucket = minioConfig.getBucket() != null ? minioConfig.getBucket() : properties.getDefaultBucket();
        
        // 初始化 MinIO 客户端
        this.minioClient = MinioClient.builder()
                .endpoint(minioConfig.getEndpoint())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();
        
        log.info("MinIO 存储初始化完成，endpoint: {}", minioConfig.getEndpoint());
    }
    
    @Override
    public StorageResult upload(UploadRequest request) {
        String bucket = request.getBucket() != null ? request.getBucket() : defaultBucket;
        String key = request.getKey();
        
        try {
            // 确保 bucket 存在
            ensureBucketExists(bucket);
            
            // 上传对象
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(request.getInputStream(), request.getSize(), -1)
                    .contentType(request.getContentType())
                    .build();
            
            ObjectWriteResponse response = minioClient.putObject(args);
            
            log.info("MinIO 存储：文件上传成功 bucket={}, key={}, etag={}", 
                    bucket, key, response.etag());
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .etag(response.etag())
                    .versionId(response.versionId())
                    .accessUrl(getUrl(bucket, key))
                    .size(request.getSize())
                    .storageType(StorageType.MINIO)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("MinIO 存储：文件上传失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.uploadFailed(key, e);
        }
    }
    
    @Override
    public InputStream download(String bucket, String key) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            
            return minioClient.getObject(args);
        } catch (Exception e) {
            log.error("MinIO 存储：文件下载失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.downloadFailed(key, e);
        }
    }
    
    @Override
    public void delete(String bucket, String key) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            
            minioClient.removeObject(args);
            log.info("MinIO 存储：文件删除成功 bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            log.error("MinIO 存储：文件删除失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.deleteFailed(key, e);
        }
    }
    
    @Override
    public void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(sourceBucket)
                    .object(sourceKey)
                    .build();
            
            CopyObjectArgs args = CopyObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(targetKey)
                    .source(source)
                    .build();
            
            minioClient.copyObject(args);
            log.info("MinIO 存储：文件复制成功 from={}/{} to={}/{}", 
                    sourceBucket, sourceKey, targetBucket, targetKey);
        } catch (Exception e) {
            log.error("MinIO 存储：文件复制失败", e);
            throw new StorageException("COPY_FAILED", "文件复制失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String bucket, String key) {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            minioClient.statObject(args);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getUrl(String bucket, String key) {
        String protocol = minioConfig.isSecure() ? "https" : "http";
        return protocol + "://" + minioConfig.getEndpoint().replaceFirst("^https?://", "") 
                + "/" + bucket + "/" + key;
    }
    
    @Override
    public String getPresignedUrl(String bucket, String key, int expireSeconds) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .method(Method.GET)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build();
            
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("MinIO 存储：获取预签名URL失败 bucket={}, key={}", bucket, key, e);
            throw new StorageException("PRESIGNED_URL_FAILED", "获取预签名URL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> listObjects(String bucket, String prefix) {
        List<String> objects = new ArrayList<>();
        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .recursive(true)
                    .build();
            
            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                objects.add(result.get().objectName());
            }
        } catch (Exception e) {
            log.error("MinIO 存储：列出对象失败 bucket={}, prefix={}", bucket, prefix, e);
            throw new StorageException("LIST_FAILED", "列出对象失败: " + e.getMessage(), e);
        }
        return objects;
    }
    
    @Override
    public StorageUsage getUsage(String bucket) {
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);
        
        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucket)
                    .recursive(true)
                    .build();
            
            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                Item item = result.get();
                totalSize.addAndGet(item.size());
                fileCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("MinIO 存储：获取使用量失败 bucket={}", bucket, e);
        }
        
        return StorageUsage.builder()
                .bucket(bucket)
                .usedBytes(totalSize.get())
                .fileCount(fileCount.get())
                .calculatedAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.MINIO;
    }
    
    @Override
    public String getDefaultBucket() {
        return defaultBucket;
    }
    
    @Override
    public String[] getSupportedFeatures() {
        return new String[]{"multipart_upload", "versioning", "presigned_url", "copy", "list"};
    }
    
    /**
     * 确保 bucket 存在
     */
    private void ensureBucketExists(String bucket) {
        try {
            BucketExistsArgs existsArgs = BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build();
            
            if (!minioClient.bucketExists(existsArgs)) {
                MakeBucketArgs makeArgs = MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build();
                minioClient.makeBucket(makeArgs);
                log.info("MinIO 存储：创建 bucket 成功 bucket={}", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO 存储：检查/创建 bucket 失败 bucket={}", bucket, e);
            throw StorageException.bucketNotFound(bucket);
        }
    }
}
