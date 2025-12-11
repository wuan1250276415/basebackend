package com.basebackend.storage.provider;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.basebackend.storage.config.StorageProperties;
import com.basebackend.storage.exception.StorageException;
import com.basebackend.storage.model.StorageResult;
import com.basebackend.storage.model.StorageUsage;
import com.basebackend.storage.model.UploadRequest;
import com.basebackend.storage.spi.StorageProvider;
import com.basebackend.storage.spi.StorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 阿里云 OSS 存储实现
 * 
 * @author BaseBackend
 */
@Slf4j
public class OssStorageProvider implements StorageProvider {
    
    private final OSS ossClient;
    private final StorageProperties.Oss ossConfig;
    private final String defaultBucket;
    
    public OssStorageProvider(StorageProperties properties) {
        this.ossConfig = properties.getOss();
        this.defaultBucket = ossConfig.getBucket() != null ? ossConfig.getBucket() : properties.getDefaultBucket();
        
        // 初始化 OSS 客户端
        this.ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
        
        log.info("阿里云 OSS 存储初始化完成，endpoint: {}", ossConfig.getEndpoint());
    }
    
    @Override
    public StorageResult upload(UploadRequest request) {
        String bucket = request.getBucket() != null ? request.getBucket() : defaultBucket;
        String key = request.getKey();
        
        try {
            // 确保 bucket 存在
            ensureBucketExists(bucket);
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            if (request.getContentType() != null) {
                metadata.setContentType(request.getContentType());
            }
            if (request.getSize() != null) {
                metadata.setContentLength(request.getSize());
            }
            if (request.getMd5Checksum() != null) {
                metadata.setContentMD5(request.getMd5Checksum());
            }
            
            // 上传
            PutObjectResult result = ossClient.putObject(bucket, key, request.getInputStream(), metadata);
            
            log.info("阿里云 OSS：文件上传成功 bucket={}, key={}, etag={}", 
                    bucket, key, result.getETag());
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .etag(result.getETag())
                    .versionId(result.getVersionId())
                    .accessUrl(getUrl(bucket, key))
                    .size(request.getSize())
                    .storageType(StorageType.ALIYUN_OSS)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("阿里云 OSS：文件上传失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.uploadFailed(key, e);
        }
    }
    
    @Override
    public InputStream download(String bucket, String key) {
        try {
            OSSObject ossObject = ossClient.getObject(bucket, key);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("阿里云 OSS：文件下载失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.downloadFailed(key, e);
        }
    }
    
    @Override
    public void delete(String bucket, String key) {
        try {
            ossClient.deleteObject(bucket, key);
            log.info("阿里云 OSS：文件删除成功 bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            log.error("阿里云 OSS：文件删除失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.deleteFailed(key, e);
        }
    }
    
    @Override
    public void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            ossClient.copyObject(sourceBucket, sourceKey, targetBucket, targetKey);
            log.info("阿里云 OSS：文件复制成功 from={}/{} to={}/{}", 
                    sourceBucket, sourceKey, targetBucket, targetKey);
        } catch (Exception e) {
            log.error("阿里云 OSS：文件复制失败", e);
            throw new StorageException("COPY_FAILED", "文件复制失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String bucket, String key) {
        try {
            return ossClient.doesObjectExist(bucket, key);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getUrl(String bucket, String key) {
        if (ossConfig.getCustomDomain() != null && !ossConfig.getCustomDomain().isEmpty()) {
            // 使用自定义域名
            return "https://" + ossConfig.getCustomDomain() + "/" + key;
        } else {
            // 使用标准 OSS 域名
            return "https://" + bucket + "." + ossConfig.getEndpoint().replaceFirst("^https?://", "") + "/" + key;
        }
    }
    
    @Override
    public String getPresignedUrl(String bucket, String key, int expireSeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            URL signedUrl = ossClient.generatePresignedUrl(bucket, key, expiration);
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("阿里云 OSS：获取预签名URL失败 bucket={}, key={}", bucket, key, e);
            throw new StorageException("PRESIGNED_URL_FAILED", "获取预签名URL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> listObjects(String bucket, String prefix) {
        List<String> objects = new ArrayList<>();
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucket);
            request.setPrefix(prefix);
            request.setMaxKeys(1000);
            
            ObjectListing listing;
            do {
                listing = ossClient.listObjects(request);
                for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                    objects.add(summary.getKey());
                }
                request.setMarker(listing.getNextMarker());
            } while (listing.isTruncated());
            
        } catch (Exception e) {
            log.error("阿里云 OSS：列出对象失败 bucket={}, prefix={}", bucket, prefix, e);
            throw new StorageException("LIST_FAILED", "列出对象失败: " + e.getMessage(), e);
        }
        return objects;
    }
    
    @Override
    public StorageUsage getUsage(String bucket) {
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);
        
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucket);
            request.setMaxKeys(1000);
            
            ObjectListing listing;
            do {
                listing = ossClient.listObjects(request);
                for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                    totalSize.addAndGet(summary.getSize());
                    fileCount.incrementAndGet();
                }
                request.setMarker(listing.getNextMarker());
            } while (listing.isTruncated());
            
        } catch (Exception e) {
            log.error("阿里云 OSS：获取使用量失败 bucket={}", bucket, e);
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
        return StorageType.ALIYUN_OSS;
    }
    
    @Override
    public String getDefaultBucket() {
        return defaultBucket;
    }
    
    @Override
    public String[] getSupportedFeatures() {
        return new String[]{"multipart_upload", "versioning", "presigned_url", "encryption", "copy", "list"};
    }
    
    /**
     * 确保 bucket 存在
     */
    private void ensureBucketExists(String bucket) {
        try {
            if (!ossClient.doesBucketExist(bucket)) {
                ossClient.createBucket(bucket);
                log.info("阿里云 OSS：创建 bucket 成功 bucket={}", bucket);
            }
        } catch (Exception e) {
            log.warn("阿里云 OSS：检查/创建 bucket 失败 bucket={}", bucket, e);
        }
    }
    
    /**
     * 关闭客户端
     */
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
