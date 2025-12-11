package com.basebackend.file.storage;

import com.basebackend.file.storage.StorageService.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 代理存储服务
 *
 * 根据请求自动路由到对应的存储服务实现
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class DelegatingStorageService implements StorageService {

    private final StorageServiceRegistry registry;

    /**
     * 根据存储类型路由到对应存储服务，未指定或未注册时回退到默认服务
     */
    private StorageService resolveService(StorageType storageType, String path, String bucket) {
        StorageService service;
        if (storageType != null) {
            if (registry.hasService(storageType)) {
                log.debug("根据存储类型路由存储服务: {}, path: {}, bucket: {}", storageType, path, bucket);
                service = registry.getService(storageType);
            } else {
                service = registry.getDefaultService();
                log.warn("存储类型 {} 未注册，回退使用默认存储服务 {}", storageType, service.getStorageType());
            }
        } else {
            service = registry.getDefaultService();
            log.debug("未指定存储类型，使用默认存储服务 {}", service.getStorageType());
        }
        return service;
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long size) {
        log.warn("使用旧版upload方法，建议使用upload(UploadRequest request)");
        StorageService service = resolveService(null, path, null);
        return service.upload(inputStream, path, contentType, size);
    }

    /**
     * 新版上传方法（推荐使用）
     *
     * @param request 上传请求
     * @return 上传结果
     */
    public UploadResult upload(UploadRequest request) {
        log.info("开始上传文件: {}, 存储类型: {}", request.getPath(), request.getStorageType());
        try {
            StorageService service = resolveService(request.getStorageType(), request.getPath(), request.getBucket());

            // 调用实际的存储服务上传
            String url = service.upload(request.getInputStream(), request.getPath(),
                    request.getContentType(), request.getSize());

            UploadResult result = UploadResult.success(
                    url,
                    request.getPath(),
                    request.getBucket(),
                    request.getContentType(),
                    request.getSize(),
                    service.getStorageType());

            log.info("文件上传成功: {}, URL: {}", request.getPath(), url);
            return result;

        } catch (Exception e) {
            log.error("文件上传失败: {}, 错误: {}", request.getPath(), e.getMessage(), e);
            return UploadResult.failure(e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        log.warn("使用旧版download方法，建议使用download(String path, String bucket)");
        StorageService service = resolveService(null, path, null);
        return service.download(path);
    }

    /**
     * 新版下载方法（推荐使用）
     *
     * @param path   文件路径
     * @param bucket 存储桶名称
     * @return 文件输入流
     */
    public InputStream download(String path, String bucket) {
        log.info("下载文件: {}, bucket: {}", path, bucket);
        // 对于Local存储，bucket可能为null
        if (bucket != null) {
            log.debug("使用bucket: {}", bucket);
        }
        StorageService service = resolveService(null, path, bucket);
        return service.download(path);
    }

    @Override
    public void delete(String path) {
        StorageService service = resolveService(null, path, null);
        service.delete(path);
    }

    @Override
    public void copy(String sourcePath, String targetPath) {
        StorageService service = resolveService(null, sourcePath, null);
        service.copy(sourcePath, targetPath);
    }

    /**
     * 复制文件（支持不同bucket）
     *
     * @param sourcePath   源路径
     * @param sourceBucket 源存储桶
     * @param targetPath   目标路径
     * @param targetBucket 目标存储桶
     */
    public void copy(String sourcePath, String sourceBucket, String targetPath, String targetBucket) {
        log.info("复制文件: {} -> {}:{}", sourcePath, targetBucket, targetPath);
        StorageService service = resolveService(null, sourcePath, sourceBucket);
        service.copy(sourcePath, targetPath);
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        StorageService service = resolveService(null, sourcePath, null);
        service.move(sourcePath, targetPath);
    }

    @Override
    public boolean exists(String path) {
        StorageService service = resolveService(null, path, null);
        return service.exists(path);
    }

    @Override
    public String getUrl(String path) {
        StorageService service = resolveService(null, path, null);
        return service.getUrl(path);
    }

    @Override
    public String getPresignedUrl(String path, int expireTime) {
        StorageService service = resolveService(null, path, null);
        return service.getPresignedUrl(path, expireTime);
    }

    @Override
    public List<String> listFiles(String prefix) {
        StorageService service = resolveService(null, prefix, null);
        return service.listFiles(prefix);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.DELEGATING;
    }
}
