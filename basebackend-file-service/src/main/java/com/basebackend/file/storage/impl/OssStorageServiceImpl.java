package com.basebackend.file.storage.impl;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.OssProperties;
import com.basebackend.file.storage.StorageService;
import com.basebackend.file.storage.StorageServiceRegistry;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
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
 * 阿里云OSS存储实现
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "aliyun_oss")
public class OssStorageServiceImpl implements StorageService {

    private final OSS ossClient;
    private final OssProperties ossProperties;
    private final StorageServiceRegistry storageServiceRegistry;

    /**
     * 初始化时自动注册到存储服务注册中心
     */
    @PostConstruct
    public void init() {
        storageServiceRegistry.registerService(StorageType.ALIYUN_OSS, this);
        log.info("阿里云OSS存储服务已注册到存储服务注册中心");
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long size) {
        try {
            // 确保bucket存在
            ensureBucketExists();

            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), path, inputStream);
            putObjectRequest.setMetadata(getObjectMetadata(contentType, size));

            // 上传文件
            PutObjectResult result = ossClient.putObject(putObjectRequest);

            log.info("OSS存储：文件上传成功 bucket={}, path={}, etag={}",
                    ossProperties.getBucketName(), path, result.getETag());
            return getUrl(path);
        } catch (Exception e) {
            log.error("OSS存储：文件上传失败 path={}", path, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), path);
            log.info("OSS存储：文件下载成功 path={}", path);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("OSS存储：文件下载失败 path={}", path, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), path);
            log.info("OSS存储：文件删除成功 path={}", path);
        } catch (Exception e) {
            log.error("OSS存储：文件删除失败 path={}", path, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public void copy(String sourcePath, String targetPath) {
        try {
            // OSS不支持直接复制，需要先下载再上传
            // 这里使用COPY API（推荐）
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
                    ossProperties.getBucketName(), sourcePath,
                    ossProperties.getBucketName(), targetPath);
            CopyObjectResult result = ossClient.copyObject(copyObjectRequest);

            log.info("OSS存储：文件复制成功 from={} to={}, etag={}",
                    sourcePath, targetPath, result.getETag());
        } catch (Exception e) {
            log.error("OSS存储：文件复制失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件复制失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        try {
            // 移动操作：复制然后删除
            copy(sourcePath, targetPath);
            delete(sourcePath);
            log.info("OSS存储：文件移动成功 from={} to={}", sourcePath, targetPath);
        } catch (Exception e) {
            log.error("OSS存储：文件移动失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件移动失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            ossClient.getObjectMetadata(ossProperties.getBucketName(), path);
            return true;
        } catch (com.aliyun.oss.ClientException e) {
            // 网络错误或客户端错误
            log.error("OSS存储：检查文件存在性失败（客户端错误）path={}", path, e);
            throw new BusinessException(CommonErrorCode.STORAGE_SERVICE_ERROR,
                    "检查文件存在性失败: " + e.getMessage(), e);
        } catch (com.aliyun.oss.ServiceException e) {
            // 服务端错误，NoSuchKey/NoSuchBucket 视为不存在，其余抛业务异常
            String errorCode = e.getErrorCode();
            if ("NoSuchKey".equals(errorCode) || "NoSuchBucket".equals(errorCode)) {
                return false;
            }
            log.error("OSS存储：检查文件存在性失败（服务错误）path={}, code={}", path, errorCode, e);
            throw new BusinessException(CommonErrorCode.STORAGE_SERVICE_ERROR,
                    "检查文件存在性失败: " + e.getErrorMessage(), e);
        } catch (Exception e) {
            log.error("OSS存储：检查文件存在性失败 path={}", path, e);
            throw new BusinessException(CommonErrorCode.STORAGE_SERVICE_ERROR,
                    "检查文件存在性失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String path) {
        if (ossProperties.getCustomDomain() != null && !ossProperties.getCustomDomain().isEmpty()) {
            // 使用自定义域名
            return "https://" + ossProperties.getCustomDomain() + "/" + path;
        } else {
            // 使用默认域名
            return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + path;
        }
    }

    @Override
    public String getPresignedUrl(String path, int expireTime) {
        try {
            // 生成签名URL
            Date expiration = new Date(System.currentTimeMillis() + expireTime * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), path);
            request.setExpiration(expiration);

            URL signedUrl = ossClient.generatePresignedUrl(request);
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("OSS存储：获取签名URL失败 path={}", path, e);
            throw new BusinessException("获取签名URL失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listFiles(String prefix) {
        List<String> files = new ArrayList<>();
        try {
            ObjectListing objectListing = ossClient.listObjects(
                    ossProperties.getBucketName(), prefix);

            for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                files.add(objectSummary.getKey());
            }
        } catch (Exception e) {
            log.error("OSS存储：列出文件失败 prefix={}", prefix, e);
            throw new BusinessException("列出文件失败: " + e.getMessage());
        }
        return files;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.ALIYUN_OSS;
    }

    /**
     * 获取对象元数据
     */
    private ObjectMetadata getObjectMetadata(String contentType, long size) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(size);
        return metadata;
    }

    /**
     * 确保bucket存在
     */
    private void ensureBucketExists() {
        try {
            boolean exists = ossClient.doesBucketExist(ossProperties.getBucketName());
            if (!exists) {
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(ossProperties.getBucketName());
                ossClient.createBucket(createBucketRequest);
                log.info("OSS存储：创建bucket成功 bucket={}", ossProperties.getBucketName());
            }
        } catch (Exception e) {
            log.error("OSS存储：检查bucket失败", e);
            throw new BusinessException("检查bucket失败: " + e.getMessage());
        }
    }
}
