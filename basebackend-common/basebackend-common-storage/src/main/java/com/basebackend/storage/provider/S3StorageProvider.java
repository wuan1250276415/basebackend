package com.basebackend.storage.provider;

import com.basebackend.storage.config.StorageProperties;
import com.basebackend.storage.exception.StorageException;
import com.basebackend.storage.model.StorageResult;
import com.basebackend.storage.model.StorageUsage;
import com.basebackend.storage.model.UploadRequest;
import com.basebackend.storage.spi.StorageProvider;
import com.basebackend.storage.spi.StorageType;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AWS S3 存储实现（使用 AWS SDK v2）
 * 
 * @author BaseBackend
 */
@Slf4j
public class S3StorageProvider implements StorageProvider {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties.S3 s3Config;
    private final String defaultBucket;
    
    public S3StorageProvider(StorageProperties properties) {
        this.s3Config = properties.getS3();
        this.defaultBucket = s3Config.getBucket() != null ? s3Config.getBucket() : properties.getDefaultBucket();
        
        // 构建凭证
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                s3Config.getAccessKeyId(), 
                s3Config.getSecretAccessKey()
        );
        
        // 构建 S3 客户端
        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(s3Config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        
        // 如果有自定义 endpoint（兼容S3的服务如MinIO）
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
            if (s3Config.isPathStyleAccessEnabled()) {
                clientBuilder.forcePathStyle(true);
            }
        }
        
        this.s3Client = clientBuilder.build();
        
        // 构建预签名器
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(s3Config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
        }
        
        this.s3Presigner = presignerBuilder.build();
        
        log.info("S3 存储初始化完成，region: {}", s3Config.getRegion());
    }
    
    @Override
    public StorageResult upload(UploadRequest request) {
        String bucket = request.getBucket() != null ? request.getBucket() : defaultBucket;
        String key = request.getKey();
        
        try {
            // 确保 bucket 存在
            ensureBucketExists(bucket);
            
            // 构建上传请求
            PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            
            if (request.getContentType() != null) {
                putBuilder.contentType(request.getContentType());
            }
            if (request.getMd5Checksum() != null) {
                putBuilder.contentMD5(request.getMd5Checksum());
            }
            
            PutObjectRequest putRequest = putBuilder.build();
            
            // 上传
            PutObjectResponse response = s3Client.putObject(putRequest, 
                    RequestBody.fromInputStream(request.getInputStream(), request.getSize()));
            
            log.info("S3 存储：文件上传成功 bucket={}, key={}, etag={}", 
                    bucket, key, response.eTag());
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .etag(response.eTag())
                    .versionId(response.versionId())
                    .accessUrl(getUrl(bucket, key))
                    .size(request.getSize())
                    .storageType(StorageType.AWS_S3)
                    .region(s3Config.getRegion())
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("S3 存储：文件上传失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.uploadFailed(key, e);
        }
    }
    
    @Override
    public InputStream download(String bucket, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw StorageException.fileNotFound(key);
        } catch (Exception e) {
            log.error("S3 存储：文件下载失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.downloadFailed(key, e);
        }
    }
    
    @Override
    public void delete(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(request);
            log.info("S3 存储：文件删除成功 bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            log.error("S3 存储：文件删除失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.deleteFailed(key, e);
        }
    }
    
    @Override
    public void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(targetBucket)
                    .destinationKey(targetKey)
                    .build();
            
            s3Client.copyObject(request);
            log.info("S3 存储：文件复制成功 from={}/{} to={}/{}", 
                    sourceBucket, sourceKey, targetBucket, targetKey);
        } catch (Exception e) {
            log.error("S3 存储：文件复制失败", e);
            throw new StorageException("COPY_FAILED", "文件复制失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getUrl(String bucket, String key) {
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            // 自定义 endpoint
            return s3Config.getEndpoint() + "/" + bucket + "/" + key;
        } else {
            // 标准 AWS S3
            return "https://" + bucket + ".s3." + s3Config.getRegion() + ".amazonaws.com/" + key;
        }
    }
    
    @Override
    public String getPresignedUrl(String bucket, String key, int expireSeconds) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expireSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("S3 存储：获取预签名URL失败 bucket={}, key={}", bucket, key, e);
            throw new StorageException("PRESIGNED_URL_FAILED", "获取预签名URL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> listObjects(String bucket, String prefix) {
        List<String> objects = new ArrayList<>();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            for (S3Object obj : response.contents()) {
                objects.add(obj.key());
            }
        } catch (Exception e) {
            log.error("S3 存储：列出对象失败 bucket={}, prefix={}", bucket, prefix, e);
            throw new StorageException("LIST_FAILED", "列出对象失败: " + e.getMessage(), e);
        }
        return objects;
    }
    
    @Override
    public StorageUsage getUsage(String bucket) {
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);
        
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .build();
            
            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(request);
                for (S3Object obj : response.contents()) {
                    totalSize.addAndGet(obj.size());
                    fileCount.incrementAndGet();
                }
                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();
            } while (response.isTruncated());
            
        } catch (Exception e) {
            log.error("S3 存储：获取使用量失败 bucket={}", bucket, e);
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
        return StorageType.AWS_S3;
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
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.headBucket(headRequest);
        } catch (NoSuchBucketException e) {
            try {
                CreateBucketRequest createRequest = CreateBucketRequest.builder()
                        .bucket(bucket)
                        .build();
                s3Client.createBucket(createRequest);
                log.info("S3 存储：创建 bucket 成功 bucket={}", bucket);
            } catch (Exception ex) {
                log.error("S3 存储：创建 bucket 失败 bucket={}", bucket, ex);
                throw StorageException.bucketNotFound(bucket);
            }
        } catch (Exception e) {
            log.warn("S3 存储：检查 bucket 失败 bucket={}", bucket, e);
        }
    }
}
