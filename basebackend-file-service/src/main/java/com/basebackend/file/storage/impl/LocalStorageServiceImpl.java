package com.basebackend.file.storage.impl;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.FileProperties;
import com.basebackend.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地存储实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements StorageService {

    private final FileProperties fileProperties;

    @Override
    public String upload(InputStream inputStream, String path, String contentType, long size) {
        try {
            Path fullPath = Paths.get(fileProperties.getUploadPath(), path);

            // 确保父目录存在
            Path parentDir = fullPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 保存文件
            Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("本地存储：文件上传成功 path={}", path);
            return getUrl(path);
        } catch (IOException e) {
            log.error("本地存储：文件上传失败 path={}", path, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            Path fullPath = Paths.get(fileProperties.getUploadPath(), path);

            if (!Files.exists(fullPath)) {
                throw new BusinessException("文件不存在: " + path);
            }

            return new FileInputStream(fullPath.toFile());
        } catch (FileNotFoundException e) {
            log.error("本地存储：文件下载失败 path={}", path, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path fullPath = Paths.get(fileProperties.getUploadPath(), path);

            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("本地存储：文件删除成功 path={}", path);
            } else {
                log.warn("本地存储：文件不存在 path={}", path);
            }
        } catch (IOException e) {
            log.error("本地存储：文件删除失败 path={}", path, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public void copy(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(fileProperties.getUploadPath(), sourcePath);
            Path target = Paths.get(fileProperties.getUploadPath(), targetPath);

            // 确保目标目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("本地存储：文件复制成功 from={} to={}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("本地存储：文件复制失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件复制失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(fileProperties.getUploadPath(), sourcePath);
            Path target = Paths.get(fileProperties.getUploadPath(), targetPath);

            // 确保目标目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("本地存储：文件移动成功 from={} to={}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("本地存储：文件移动失败 from={} to={}", sourcePath, targetPath, e);
            throw new BusinessException("文件移动失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        Path fullPath = Paths.get(fileProperties.getUploadPath(), path);
        return Files.exists(fullPath);
    }

    @Override
    public String getUrl(String path) {
        return fileProperties.getAccessPrefix() + "/" + path;
    }

    @Override
    public String getPresignedUrl(String path, int expireTime) {
        // 本地存储不支持签名URL，直接返回普通URL
        return getUrl(path);
    }

    @Override
    public List<String> listFiles(String prefix) {
        List<String> files = new ArrayList<>();
        Path dirPath = Paths.get(fileProperties.getUploadPath(), prefix);

        try {
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.filter(Files::isRegularFile)
                         .forEach(path -> {
                             String relativePath = Paths.get(fileProperties.getUploadPath())
                                                        .relativize(path)
                                                        .toString();
                             files.add(relativePath.replace("\\", "/"));
                         });
                }
            }
        } catch (IOException e) {
            log.error("本地存储：列出文件失败 prefix={}", prefix, e);
            throw new BusinessException("列出文件失败: " + e.getMessage());
        }

        return files;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }
}
