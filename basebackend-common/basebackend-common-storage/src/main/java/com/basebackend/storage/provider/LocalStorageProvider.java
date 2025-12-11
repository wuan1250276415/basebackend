package com.basebackend.storage.provider;

import com.basebackend.storage.config.StorageProperties;
import com.basebackend.storage.exception.StorageException;
import com.basebackend.storage.model.StorageResult;
import com.basebackend.storage.model.StorageUsage;
import com.basebackend.storage.model.UploadRequest;
import com.basebackend.storage.spi.StorageProvider;
import com.basebackend.storage.spi.StorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 本地文件系统存储实现
 * 
 * @author BaseBackend
 */
@Slf4j
public class LocalStorageProvider implements StorageProvider {
    
    private final StorageProperties.Local localConfig;
    private final String defaultBucket;
    
    public LocalStorageProvider(StorageProperties properties) {
        this.localConfig = properties.getLocal();
        this.defaultBucket = properties.getDefaultBucket();
        
        // 初始化时确保基础目录存在
        initBaseDirectory();
    }
    
    private void initBaseDirectory() {
        try {
            Path basePath = Paths.get(localConfig.getBasePath());
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("创建本地存储目录: {}", basePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("无法创建本地存储目录", e);
            throw StorageException.configurationError("无法创建存储目录: " + e.getMessage());
        }
    }
    
    @Override
    public StorageResult upload(UploadRequest request) {
        String bucket = request.getBucket() != null ? request.getBucket() : defaultBucket;
        String key = request.getKey();
        
        try {
            Path fullPath = resolvePath(bucket, key);
            
            // 确保父目录存在
            Path parentDir = fullPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 保存文件
            Files.copy(request.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("本地存储：文件上传成功 bucket={}, key={}", bucket, key);
            
            return StorageResult.builder()
                    .success(true)
                    .bucket(bucket)
                    .key(key)
                    .location(fullPath.toAbsolutePath().toString())
                    .accessUrl(getUrl(bucket, key))
                    .size(request.getSize())
                    .storageType(StorageType.LOCAL)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
        } catch (IOException e) {
            log.error("本地存储：文件上传失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.uploadFailed(key, e);
        }
    }
    
    @Override
    public InputStream download(String bucket, String key) {
        try {
            Path fullPath = resolvePath(bucket, key);
            
            if (!Files.exists(fullPath)) {
                throw StorageException.fileNotFound(key);
            }
            
            return new FileInputStream(fullPath.toFile());
        } catch (IOException e) {
            log.error("本地存储：文件下载失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.downloadFailed(key, e);
        }
    }
    
    @Override
    public void delete(String bucket, String key) {
        try {
            Path fullPath = resolvePath(bucket, key);
            
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("本地存储：文件删除成功 bucket={}, key={}", bucket, key);
            } else {
                log.warn("本地存储：文件不存在 bucket={}, key={}", bucket, key);
            }
        } catch (IOException e) {
            log.error("本地存储：文件删除失败 bucket={}, key={}", bucket, key, e);
            throw StorageException.deleteFailed(key, e);
        }
    }
    
    @Override
    public void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            Path source = resolvePath(sourceBucket, sourceKey);
            Path target = resolvePath(targetBucket, targetKey);
            
            if (!Files.exists(source)) {
                throw StorageException.fileNotFound(sourceKey);
            }
            
            // 确保目标目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }
            
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("本地存储：文件复制成功 from={}/{} to={}/{}", 
                    sourceBucket, sourceKey, targetBucket, targetKey);
        } catch (IOException e) {
            log.error("本地存储：文件复制失败", e);
            throw new StorageException("COPY_FAILED", "文件复制失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String bucket, String key) {
        Path fullPath = resolvePath(bucket, key);
        return Files.exists(fullPath);
    }
    
    @Override
    public String getUrl(String bucket, String key) {
        return localConfig.getUrlPrefix() + "/" + bucket + "/" + key;
    }
    
    @Override
    public String getPresignedUrl(String bucket, String key, int expireSeconds) {
        // 本地存储不支持预签名URL，返回普通URL
        log.debug("本地存储不支持预签名URL，返回普通URL");
        return getUrl(bucket, key);
    }
    
    @Override
    public List<String> listObjects(String bucket, String prefix) {
        List<String> files = new ArrayList<>();
        Path dirPath = resolvePath(bucket, prefix);
        
        try {
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                Path bucketPath = resolvePath(bucket, "");
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.filter(Files::isRegularFile)
                         .forEach(path -> {
                             String relativePath = bucketPath.relativize(path).toString();
                             files.add(relativePath.replace("\\", "/"));
                         });
                }
            }
        } catch (IOException e) {
            log.error("本地存储：列出文件失败 bucket={}, prefix={}", bucket, prefix, e);
            throw new StorageException("LIST_FAILED", "列出文件失败: " + e.getMessage(), e);
        }
        
        return files;
    }
    
    @Override
    public StorageUsage getUsage(String bucket) {
        Path bucketPath = resolvePath(bucket, "");
        
        if (!Files.exists(bucketPath)) {
            return StorageUsage.builder()
                    .bucket(bucket)
                    .usedBytes(0L)
                    .fileCount(0L)
                    .calculatedAt(LocalDateTime.now())
                    .build();
        }
        
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);
        
        try {
            Files.walkFileTree(bucketPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    totalSize.addAndGet(attrs.size());
                    fileCount.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("本地存储：获取使用量失败 bucket={}", bucket, e);
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
        return StorageType.LOCAL;
    }
    
    @Override
    public String getDefaultBucket() {
        return defaultBucket;
    }
    
    @Override
    public String[] getSupportedFeatures() {
        return new String[]{"copy", "move", "list", "usage"};
    }
    
    /**
     * 解析完整路径
     */
    private Path resolvePath(String bucket, String key) {
        return Paths.get(localConfig.getBasePath(), bucket, key);
    }
}
