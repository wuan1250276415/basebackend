package com.basebackend.backup.infrastructure.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.StorageUsage;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * 阿里云 OSS 存储提供者
 */
@Slf4j
public class OssStorageProvider implements StorageProvider {

    private final BackupProperties backupProperties;
    private volatile OSS ossClient;
    private final java.util.concurrent.locks.ReentrantLock clientLock = new java.util.concurrent.locks.ReentrantLock();

    private static final String STORAGE_TYPE = "oss";

    public OssStorageProvider(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    private OSS getOssClient() {
        if (ossClient != null) {
            return ossClient;
        }
        clientLock.lock();
        try {
            if (ossClient != null) {
                return ossClient;
            }
            BackupProperties.Storage.Oss ossConfig = backupProperties.getStorage().getOss();
            log.info("初始化阿里云OSS客户端, Endpoint: {}", ossConfig.getEndpoint());
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKey(),
                    ossConfig.getSecretKey());
            return ossClient;
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    public StorageResult upload(UploadRequest request) throws Exception {
        OSS client = getOssClient();
        String bucket = request.getBucket();
        String key = request.getKey();
        long size = request.getSize();

        log.info("开始上传文件到阿里云OSS: bucket={}, key={}, size={} bytes", bucket, key, size);

        try {
            // 读取完整数据用于上传和校验
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            try (InputStream in = request.getInputStream()) {
                while ((bytesRead = in.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                    totalBytes += bytesRead;
                }
            }
            byte[] fileData = buffer.toByteArray();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileData.length);
            if (request.getContentType() != null) {
                metadata.setContentType(request.getContentType());
            }

            PutObjectResult result = client.putObject(
                    bucket, key, new ByteArrayInputStream(fileData), metadata);

            String md5 = calculateMD5(fileData);

            log.info("阿里云OSS上传成功: {}, 大小: {} bytes, ETag: {}", key, totalBytes, result.getETag());

            return StorageResult.builder()
                    .bucket(bucket)
                    .key(key)
                    .location(key)
                    .size(totalBytes)
                    .storageType(STORAGE_TYPE)
                    .success(true)
                    .createdAt(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .etag(result.getETag())
                    .metadata(Map.of("md5", md5))
                    .build();

        } catch (OSSException e) {
            log.error("阿里云OSS上传失败: bucket={}, key={}, errorCode={}", bucket, key, e.getErrorCode(), e);
            throw new Exception("阿里云OSS上传失败: " + e.getErrorMessage(), e);
        }
    }

    @Override
    public InputStream download(String bucket, String key) throws Exception {
        OSS client = getOssClient();
        log.debug("下载阿里云OSS文件: bucket={}, key={}", bucket, key);

        try {
            OSSObject ossObject = client.getObject(bucket, key);
            return ossObject.getObjectContent();
        } catch (OSSException e) {
            log.error("阿里云OSS下载失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("阿里云OSS下载失败: " + e.getErrorMessage(), e);
        }
    }

    @Override
    public boolean delete(String bucket, String key) throws Exception {
        OSS client = getOssClient();
        log.debug("删除阿里云OSS文件: bucket={}, key={}", bucket, key);

        try {
            client.deleteObject(bucket, key);
            log.info("阿里云OSS文件删除成功: {}", key);
            return true;
        } catch (OSSException e) {
            log.error("阿里云OSS删除失败: bucket={}, key={}", bucket, key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String bucket, String key) throws Exception {
        OSS client = getOssClient();
        try {
            return client.doesObjectExist(bucket, key);
        } catch (OSSException e) {
            log.error("阿里云OSS检查文件存在性失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("检查文件存在性失败: " + e.getErrorMessage(), e);
        }
    }

    @Override
    public boolean verify(String bucket, String key, String expectedMd5, String expectedSha256) throws Exception {
        OSS client = getOssClient();

        try {
            // ETag 验证
            if (expectedMd5 != null) {
                ObjectMetadata metadata = client.getObjectMetadata(bucket, key);
                String actualEtag = metadata.getETag();
                if (actualEtag != null) {
                    actualEtag = actualEtag.replace("\"", "");
                }
                if (!expectedMd5.equalsIgnoreCase(actualEtag)) {
                    log.warn("MD5校验失败: expected={}, actual={}", expectedMd5, actualEtag);
                    return false;
                }
            }

            // SHA256 验证需下载完整文件
            if (expectedSha256 != null) {
                try (InputStream is = download(bucket, key)) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        md.update(buf, 0, n);
                    }
                    String actualSha256 = bytesToHex(md.digest());
                    if (!expectedSha256.equals(actualSha256)) {
                        log.warn("SHA256校验失败: expected={}, actual={}", expectedSha256, actualSha256);
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("阿里云OSS文件验证失败: bucket={}, key={}", bucket, key, e);
            return false;
        }
    }

    @Override
    public StorageUsage getUsage(String bucket) throws Exception {
        OSS client = getOssClient();

        try {
            long totalSize = 0;
            long objectCount = 0;
            String nextMarker = null;

            do {
                ListObjectsRequest listRequest = new ListObjectsRequest(bucket);
                listRequest.setMaxKeys(1000);
                if (nextMarker != null) {
                    listRequest.setMarker(nextMarker);
                }

                ObjectListing listing = client.listObjects(listRequest);
                for (OSSObjectSummary summary : listing.getObjectSummaries()) {
                    totalSize += summary.getSize();
                    objectCount++;
                }

                nextMarker = listing.isTruncated() ? listing.getNextMarker() : null;
            } while (nextMarker != null);

            return StorageUsage.builder()
                    .usedBytes(totalSize)
                    .totalBytes(-1L)
                    .usedHumanReadable(formatBytes(totalSize))
                    .totalHumanReadable("按需付费")
                    .usedPercentage(0.0)
                    .objectCount(objectCount)
                    .bucketCount(1L)
                    .storageType(STORAGE_TYPE)
                    .build();

        } catch (OSSException e) {
            log.error("获取阿里云OSS使用量失败: bucket={}", bucket, e);
            throw new Exception("获取阿里云OSS使用量失败: " + e.getErrorMessage(), e);
        }
    }

    @Override
    public String[] getSupportedFeatures() {
        return new String[]{
                "multipart_upload",
                "object_versioning",
                "encryption",
                "presigned_url",
                "lifecycle_management"
        };
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String getPresignedUrl(String bucket, String key, int expirationMinutes) throws Exception {
        OSS client = getOssClient();

        try {
            Date expiration = new Date(System.currentTimeMillis() + expirationMinutes * 60L * 1000L);
            URL url = client.generatePresignedUrl(bucket, key, expiration);
            return url.toString();
        } catch (OSSException e) {
            log.error("生成预签名URL失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("生成预签名URL失败: " + e.getErrorMessage(), e);
        }
    }

    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return bytesToHex(md.digest(data));
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }

    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.debug("阿里云OSS客户端已关闭");
        }
    }
}
