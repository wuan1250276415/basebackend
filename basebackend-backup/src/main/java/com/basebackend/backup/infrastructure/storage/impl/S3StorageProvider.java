package com.basebackend.backup.infrastructure.storage.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.StorageUsage;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * S3兼容云存储提供者
 * 支持AWS S3、阿里云OSS、腾讯云COS、MinIO等S3协议存储
 */
@Slf4j
public class S3StorageProvider implements StorageProvider {

    private final BackupProperties backupProperties;

    public S3StorageProvider(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    private S3Client s3Client;

    private S3Presigner s3Presigner;

    private static final String STORAGE_TYPE = "s3";

    /**
     * 初始化S3客户端
     */
    private void initS3Client() {
        if (s3Client != null) {
            return;
        }

        BackupProperties.Storage.S3 s3Config = backupProperties.getStorage().getS3();
        log.info("初始化S3客户端, Endpoint: {}", s3Config.getEndpoint());

        // 构建S3客户端
        S3ClientBuilder clientBuilder = S3Client.builder();

        // 设置区域
        if (s3Config.getRegion() != null && !s3Config.getRegion().isEmpty()) {
            clientBuilder.region(Region.of(s3Config.getRegion()));
        }

        // 如果配置了endpoint（OSS、COS、MinIO等），使用自定义endpoint
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
        }

        // 设置认证信息
        if (s3Config.getAccessKey() != null && !s3Config.getAccessKey().isEmpty() &&
            s3Config.getSecretKey() != null && !s3Config.getSecretKey().isEmpty()) {

            AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey())
            );
            clientBuilder.credentialsProvider(credentialsProvider);
        }

        s3Client = clientBuilder.build();

        // 初始化预签名URL生成器
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            s3Presigner = S3Presigner.builder()
                .region(Region.of(s3Config.getRegion()))
                .endpointOverride(URI.create(s3Config.getEndpoint()))
                .credentialsProvider(s3Client.serviceClientConfiguration().credentialsProvider())
                .build();
        } else {
            s3Presigner = S3Presigner.create();
        }
    }

    @Override
    public StorageResult upload(UploadRequest request) throws Exception {
        initS3Client();

        String bucket = request.getBucket();
        String key = request.getKey();
        InputStream inputStream = request.getInputStream();
        long size = request.getSize();

        log.info("开始上传文件到S3: bucket={}, key={}, size={} bytes", bucket, key, size);

        try {
            BackupProperties.Storage.S3 s3Config = backupProperties.getStorage().getS3();

            // 判断是否使用多部分上传
            boolean useMultipart = request.isMultipart() || size > s3Config.getMultipartChunkSize();

            StorageResult result;
            if (useMultipart) {
                result = multipartUpload(bucket, key, inputStream, size, request);
            } else {
                result = simpleUpload(bucket, key, inputStream, size, request);
            }

            log.info("S3文件上传成功: {}, 大小: {} bytes", key, result.getSize());
            return result;

        } catch (SdkException e) {
            log.error("S3上传失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("S3上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简单上传（小文件）
     */
    private StorageResult simpleUpload(String bucket, String key, InputStream inputStream,
                                      long size, UploadRequest request) throws IOException {
        @Cleanup("close") ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        long totalBytes = 0;

        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
            totalBytes += bytesRead;
        }

        byte[] fileData = buffer.toByteArray();

        // 构建上传请求
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(request.getContentType())
            .metadata(request.getMetadata())
            .build();

        // 计算MD5用于验证
        String md5Hash = calculateMD5(fileData);
        putObjectRequest = putObjectRequest.toBuilder()
            .contentMD5(md5Hash)
            .build();

        // 执行上传
        PutObjectResponse response = s3Client.putObject(
            putObjectRequest,
            RequestBody.fromBytes(fileData)
        );

        // 构建结果
        return StorageResult.builder()
            .bucket(bucket)
            .key(key)
            .location(key)
            .size(totalBytes)
            .storageType(STORAGE_TYPE)
            .success(true)
            .createdAt(LocalDateTime.now())
            .lastModified(LocalDateTime.now())
            .etag(response.eTag())
            .metadata(Map.of("md5", md5Hash))
            .build();
    }

    /**
     * 多部分上传（大文件）
     */
    private StorageResult multipartUpload(String bucket, String key, InputStream inputStream,
                                        long size, UploadRequest request) throws IOException {
        BackupProperties.Storage.S3 s3Config = backupProperties.getStorage().getS3();

        log.info("使用多部分上传: bucket={}, key={}, size={} bytes", bucket, key, size);

        // 1. 初始化多部分上传
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(request.getContentType())
            .metadata(request.getMetadata())
            .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        log.info("创建多部分上传, uploadId={}", uploadId);

        try {
            // 2. 上传分块
            List<CompletedPart> completedParts = new ArrayList<>();
            @Cleanup("close") ByteArrayOutputStream partBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[s3Config.getMultipartChunkSize().intValue()];
            int partNumber = 1;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                partBuffer.write(buffer, 0, bytesRead);

                // 当缓冲区达到分块大小或到达文件末尾时上传
                if (partBuffer.size() >= s3Config.getMultipartChunkSize() ||
                    (bytesRead = inputStream.read(buffer)) == -1) {

                    byte[] partData = partBuffer.toByteArray();

                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) partData.length)
                        .build();

                    UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                        uploadPartRequest,
                        RequestBody.fromBytes(partData)
                    );

                    String etag = uploadPartResponse.eTag();
                    completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(etag)
                        .build());

                    log.debug("上传分块完成: part={}, size={} bytes, etag={}",
                        partNumber, partData.length, etag);

                    partNumber++;
                    partBuffer.reset();
                }
            }

            // 3. 完成多部分上传
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build())
                .build();

            CompleteMultipartUploadResponse completeResponse =
                s3Client.completeMultipartUpload(completeRequest);

            log.info("多部分上传完成: {}", key);

            // 4. 获取对象信息
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            return StorageResult.builder()
                .bucket(bucket)
                .key(key)
                .location(key)
                .size(size)
                .storageType(STORAGE_TYPE)
                .success(true)
                .createdAt(LocalDateTime.now())
                .lastModified(LocalDateTime.now())
                .etag(completeResponse.eTag())
                .metadata(Map.of(
                    "uploadId", uploadId,
                    "parts", String.valueOf(completedParts.size()),
                    "contentLength", String.valueOf(headResponse.contentLength())
                ))
                .build();

        } catch (Exception e) {
            // 中止多部分上传
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build();

            s3Client.abortMultipartUpload(abortRequest);
            log.warn("中止多部分上传: uploadId={}", uploadId, e);

            throw e;
        }
    }

    @Override
    public InputStream download(String bucket, String key) throws Exception {
        initS3Client();

        log.debug("下载S3文件: bucket={}, key={}", bucket, key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            return s3Object;

        } catch (SdkException e) {
            log.error("S3下载失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("S3下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String bucket, String key) throws Exception {
        initS3Client();

        log.debug("删除S3文件: bucket={}, key={}", bucket, key);

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);
            log.info("S3文件删除成功: {}", key);
            return true;

        } catch (SdkException e) {
            log.error("S3删除失败: bucket={}, key={}", bucket, key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String bucket, String key) throws Exception {
        initS3Client();

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            log.error("检查S3文件存在性失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("检查S3文件存在性失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verify(String bucket, String key, String expectedMd5, String expectedSha256) throws Exception {
        initS3Client();

        try {
            // 获取对象元数据
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            String actualMd5 = headResponse.eTag();

            // MD5验证（如果提供）
            if (expectedMd5 != null) {
                // ETag可能包含引号，需要去除
                actualMd5 = actualMd5.replace("\"", "");
                boolean md5Match = expectedMd5.equals(actualMd5);

                if (!md5Match) {
                    log.warn("MD5校验失败: expected={}, actual={}", expectedMd5, actualMd5);
                    return false;
                }
            }

            // SHA256验证需要下载完整文件（成本较高）
            if (expectedSha256 != null) {
                try (InputStream is = download(bucket, key)) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        md.update(buffer, 0, bytesRead);
                    }
                    String actualSha256 = bytesToHex(md.digest());
                    boolean sha256Match = expectedSha256.equals(actualSha256);

                    if (!sha256Match) {
                        log.warn("SHA256校验失败: expected={}, actual={}", expectedSha256, actualSha256);
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            log.error("S3文件验证失败: bucket={}, key={}", bucket, key, e);
            return false;
        }
    }

    @Override
    public StorageUsage getUsage(String bucket) throws Exception {
        initS3Client();

        try {
            // 统计bucket中的对象数量和大小
            long totalSize = 0;
            long objectCount = 0;

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();

            ListObjectsV2Response listResponse;
            do {
                listResponse = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : listResponse.contents()) {
                    totalSize += s3Object.size();
                    objectCount++;
                }

                listRequest = listRequest.toBuilder()
                    .continuationToken(listResponse.nextContinuationToken())
                    .build();

            } while (listResponse.isTruncated());

            return StorageUsage.builder()
                .usedBytes(totalSize)
                .totalBytes(-1L) // S3没有固定配额限制
                .usedHumanReadable(formatBytes(totalSize))
                .totalHumanReadable("按需付费")
                .usedPercentage(0.0)
                .objectCount(objectCount)
                .bucketCount(1L)
                .storageType(STORAGE_TYPE)
                .build();

        } catch (SdkException e) {
            log.error("获取S3使用量失败: bucket={}", bucket, e);
            throw new Exception("获取S3使用量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] getSupportedFeatures() {
        return new String[]{
            "multipart_upload",
            "object_versioning",
            "encryption",
            "presigned_url",
            "lifecycle_management",
            "cors"
        };
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String getPresignedUrl(String bucket, String key, int expirationMinutes) throws Exception {
        initS3Client();

        try {
            log.debug("生成预签名URL: bucket={}, key={}, expiration={} minutes",
                bucket, key, expirationMinutes);

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build())
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            URL signedUrl = presignedRequest.url();

            return signedUrl.toString();

        } catch (Exception e) {
            log.error("生成预签名URL失败: bucket={}, key={}", bucket, key, e);
            throw new Exception("生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算MD5校验和
     */
    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 清理资源（Bean销毁时调用）
     */
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
            log.debug("S3Client 已关闭");
        }
        if (s3Presigner != null) {
            s3Presigner.close();
            log.debug("S3Presigner 已关闭");
        }
    }
}
