package com.basebackend.file.storage.adapter;

import com.basebackend.file.storage.StorageService;
import com.basebackend.storage.model.StorageResult;
import com.basebackend.storage.model.StorageUsage;
import com.basebackend.storage.model.UploadRequest;
import com.basebackend.storage.spi.StorageProvider;
import com.basebackend.storage.spi.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;

/**
 * 存储服务适配器
 * <p>
 * 将新的 StorageProvider 接口适配为旧的 StorageService 接口，
 * 保持向后兼容性，允许逐步迁移。
 *
 * @author BaseBackend
 */
@Slf4j
@RequiredArgsConstructor
public class StorageServiceAdapter implements StorageService {

    private final StorageProvider storageProvider;

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long size) {
        UploadRequest request = UploadRequest.builder()
                .key(path)
                .inputStream(inputStream)
                .contentType(contentType)
                .size(size)
                .build();

        StorageResult result = storageProvider.upload(request);
        return result.getAccessUrl();
    }

    @Override
    public InputStream download(String path) {
        return storageProvider.download(path);
    }

    @Override
    public void delete(String path) {
        storageProvider.delete(path);
    }

    @Override
    public void copy(String sourcePath, String targetPath) {
        storageProvider.copy(sourcePath, targetPath);
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        storageProvider.move(sourcePath, targetPath);
    }

    @Override
    public boolean exists(String path) {
        return storageProvider.exists(path);
    }

    @Override
    public String getUrl(String path) {
        return storageProvider.getUrl(path);
    }

    @Override
    public String getPresignedUrl(String path, int expireTime) {
        return storageProvider.getPresignedUrl(path, expireTime);
    }

    @Override
    public List<String> listFiles(String prefix) {
        return storageProvider.listObjects(prefix);
    }

    @Override
    public StorageType getStorageType() {
        // 转换为公共模块的 StorageType
        com.basebackend.storage.spi.StorageType commonType = storageProvider.getStorageType();

        // 映射到 file-service 的枚举（如果类型相同可直接返回）
        switch (commonType) {
            case LOCAL:
                return StorageType.LOCAL;
            case MINIO:
                return StorageType.MINIO;
            case ALIYUN_OSS:
                return StorageType.ALIYUN_OSS;
            case AWS_S3:
                return StorageType.AWS_S3;
            default:
                return StorageType.LOCAL;
        }
    }
}
