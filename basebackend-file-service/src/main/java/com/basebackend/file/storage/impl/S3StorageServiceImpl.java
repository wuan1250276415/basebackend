package com.basebackend.file.storage.impl;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.S3Properties;
import com.basebackend.file.storage.StorageService;
import com.basebackend.file.storage.StorageServiceRegistry;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AWS S3存储实现
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "aws_s3")
public class S3StorageServiceImpl implements StorageService {

    private final AmazonS3 s3Client;
    private final S3Properties s3Properties;
    private final StorageServiceRegistry storageServiceRegistry;

    /**
     * 初始化时自动注册到存储服务注册中心
     */
    @PostConstruct
    public void init() {
        storageServiceRegistry.registerService(StorageType.AWS_S3, this);
        log.info("AWS S3存储服务已注册到存储服务注册中心");
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long size) {
        try {
            // 确保bucket存在
            ensureBucketExists();

            // 创建上传请求
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(size);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    s3Properties.getBucketName(), path, inputStream, metadata);

            // 上传文件
            PutObjectResult result = s3Client.putObject(putObjectRequest);

            log.info("S3存储：文件上传成功 bucket={}, path={}, etag={}",
                    s3Properties.getBucketName(), path, result.getETag());
            return getUrl(path);
        } catch (Exception e) {
            log.error("S3存储：文件上传失败 path={}", path, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            S3Object s3Object = s3Client.getObject(s3Properties.getBucketName(), path);
            log.info("S3存储：文件下载成功 path={}", path);
            return s3Object.getObjectContent();
        } catch (Exception e) {
            log.error("S3存储：文件下载失败 path={}", path, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            s3Client.deleteObject(s3Properties.getBucketName(), path);
            log.info("S3存储：文件删除成功 path={}", path);
        } catch (Exception e) {
            log.error("S3存储：文件删除失败 path={}", path, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public void copy(String sourcePath, String targetPath) {
        try {
            // 使用S3的COPY API
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
                    s3Properties.getBucketName(), sourcePath,
                    s3Properties.getBucketName(), targetPath);
            CopyObjectResult result = s3Client.copyObject(copyObjectRequest);

            log.info("S3存储：文件复制成功 from={} to={}, etag={}",
                    sourcePath, targetPath, result.getETag());
        } catch (Exception e) {
            log.error("S3存储：文件复制失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件复制失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        try {
            // 移动操作：复制然后删除
            copy(sourcePath, targetPath);
            delete(sourcePath);
            log.info("S3存储：文件移动成功 from={} to={}", sourcePath, targetPath);
        } catch (Exception e) {
            log.error("S3存储：文件移动失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件移动失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            s3Client.getObjectMetadata(s3Properties.getBucketName(), path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getUrl(String path) {
        if (s3Properties.getCustomDomain() != null && !s3Properties.getCustomDomain().isEmpty()) {
            // 使用自定义域名（CloudFront或自定义S3域名）
            String protocol = s3Properties.getProtocol() != null ? s3Properties.getProtocol() : "https";
            return protocol + "://" + s3Properties.getCustomDomain() + "/" + path;
        } else if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isEmpty()) {
            // 使用自定义endpoint
            String protocol = s3Properties.getProtocol() != null ? s3Properties.getProtocol() : "https";
            // 如果是标准S3 endpoint，构造标准URL
            if (s3Properties.getEndpoint().contains("amazonaws.com")) {
                return protocol + "://" + s3Properties.getBucketName() + ".s3." + s3Properties.getRegion() +
                        ".amazonaws.com/" + path;
            } else {
                // 如果是自定义endpoint（MinIO或其他S3兼容服务）
                return protocol + "://" + s3Properties.getEndpoint().replaceFirst("^https?://", "") +
                        "/" + s3Properties.getBucketName() + "/" + path;
            }
        } else {
            // 使用默认AWS S3域名
            return "https://" + s3Properties.getBucketName() + ".s3." + s3Properties.getRegion() +
                    ".amazonaws.com/" + path;
        }
    }

    @Override
    public String getPresignedUrl(String path, int expireTime) {
        try {
            // 生成签名URL
            Date expiration = new Date(System.currentTimeMillis() + expireTime * 1000L);
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                    s3Properties.getBucketName(), path)
                    .withExpiration(expiration)
                    .withMethod(HttpMethod.GET);

            URL signedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("S3存储：获取签名URL失败 path={}", path, e);
            throw new BusinessException("获取签名URL失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listFiles(String prefix) {
        List<String> files = new ArrayList<>();
        try {
            ObjectListing objectListing = s3Client.listObjects(
                    s3Properties.getBucketName(), prefix);

            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                files.add(objectSummary.getKey());
            }
        } catch (Exception e) {
            log.error("S3存储：列出文件失败 prefix={}", prefix, e);
            throw new BusinessException("列出文件失败: " + e.getMessage());
        }
        return files;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.AWS_S3;
    }

    /**
     * 确保bucket存在
     */
    private void ensureBucketExists() {
        try {
            if (!s3Client.doesBucketExistV2(s3Properties.getBucketName())) {
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(s3Properties.getBucketName());
                s3Client.createBucket(createBucketRequest);
                log.info("S3存储：创建bucket成功 bucket={}", s3Properties.getBucketName());
            }
        } catch (Exception e) {
            log.error("S3存储：检查bucket失败", e);
            throw new BusinessException("检查bucket失败: " + e.getMessage());
        }
    }
}
