package com.basebackend.backup.infrastructure.storage.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.StorageUsage;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 本地文件系统存储提供者
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "backup.storage.local.enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.context.annotation.Primary
public class LocalStorageProvider implements StorageProvider {

    @Autowired
    private BackupProperties backupProperties;

    private static final String STORAGE_TYPE = "local";

    @Override
    public StorageResult upload(UploadRequest request) throws Exception {
        String bucket = request.getBucket();
        String key = request.getKey();
        InputStream inputStream = request.getInputStream();
        long size = request.getSize();

        // 构建本地文件路径并进行安全检查
        Path basePath = Paths.get(backupProperties.getStorage().getLocal().getBasePath());
        Path targetPath = resolveAndValidatePath(basePath, bucket, key);

        // 确保目录存在
        Files.createDirectories(targetPath.getParent());

        // 写入文件
        long bytesTransferred = 0;
        try (InputStream in = inputStream;
             OutputStream out = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesTransferred += bytesRead;
            }
        }

        // 计算校验和
        String md5 = calculateMD5(targetPath);
        String sha256 = calculateSHA256(targetPath);

        log.info("本地文件上传成功: {}, 大小: {} bytes, MD5: {}", targetPath, bytesTransferred, md5);

        return StorageResult.builder()
            .bucket(bucket)
            .location(targetPath.toString())
            .key(key)
            .size(bytesTransferred)
            .storageType(STORAGE_TYPE)
            .createdAt(LocalDateTime.now())
            .lastModified(LocalDateTime.now())
            .metadata(java.util.Map.of("md5", md5, "sha256", sha256))
            .success(true)
            .build();
    }

    @Override
    public InputStream download(String bucket, String key) throws Exception {
        Path basePath = Paths.get(backupProperties.getStorage().getLocal().getBasePath());
        Path targetPath = resolveAndValidatePath(basePath, bucket, key);

        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("文件不存在: " + targetPath);
        }

        return Files.newInputStream(targetPath);
    }

    @Override
    public boolean delete(String bucket, String key) throws Exception {
        Path basePath = Paths.get(backupProperties.getStorage().getLocal().getBasePath());
        Path targetPath = resolveAndValidatePath(basePath, bucket, key);

        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
            log.info("本地文件删除成功: {}", targetPath);
            return true;
        }

        return false;
    }

    @Override
    public boolean exists(String bucket, String key) throws Exception {
        Path basePath = Paths.get(backupProperties.getStorage().getLocal().getBasePath());
        Path targetPath = resolveAndValidatePath(basePath, bucket, key);
        return Files.exists(targetPath);
    }

    @Override
    public boolean verify(String bucket, String key, String expectedMd5, String expectedSha256) throws Exception {
        Path basePath = Paths.get(backupProperties.getStorage().getLocal().getBasePath());
        Path targetPath = resolveAndValidatePath(basePath, bucket, key);

        if (!Files.exists(targetPath)) {
            return false;
        }

        String actualMd5 = calculateMD5(targetPath);
        String actualSha256 = calculateSHA256(targetPath);

        boolean md5Match = expectedMd5 == null || expectedMd5.equals(actualMd5);
        boolean sha256Match = expectedSha256 == null || expectedSha256.equals(actualSha256);

        return md5Match && sha256Match;
    }

    /**
     * 解析并验证路径，防止路径遍历攻击
     *
     * @param basePath 基础路径
     * @param bucket 存储桶名称
     * @param key 对象键名
     * @return 规范化后的完整路径
     * @throws IllegalArgumentException 路径无效时抛出异常
     */
    private Path resolveAndValidatePath(Path basePath, String bucket, String key) throws IllegalArgumentException {
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new IllegalArgumentException("存储桶名称不能为空");
        }

        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("对象键名不能为空");
        }

        // 验证bucket和key不包含路径遍历序列
        if (containsPathTraversal(bucket) || containsPathTraversal(key)) {
            throw new IllegalArgumentException("路径包含非法字符: " + bucket + "/" + key);
        }

        // 转换为绝对路径
        Path absoluteBasePath = basePath.toAbsolutePath().normalize();

        // 构建目标路径并转换为绝对路径
        Path targetPath = absoluteBasePath.resolve(bucket).resolve(key).toAbsolutePath().normalize();

        // 验证目标路径是否在基础路径范围内（防止../遍历）
        if (!targetPath.startsWith(absoluteBasePath)) {
            throw new IllegalArgumentException("路径遍历攻击检测: " + bucket + "/" + key);
        }

        return targetPath;
    }

    /**
     * 检查字符串是否包含路径遍历序列
     */
    private boolean containsPathTraversal(String path) {
        if (path == null) {
            return false;
        }

        // 检查常见的路径遍历序列
        return path.contains("..") ||
               path.contains("~") ||
               path.contains("${") ||  // 防止变量注入
               path.contains("#") ||
               path.contains("%");
    }

    @Override
    public StorageUsage getUsage(String bucket) throws Exception {
        Path basePath = Paths.get(backupProperties.getStorage().getLocal().getBasePath());

        long usedBytes = 0;
        long objectCount = 0;

        if (bucket != null) {
            Path bucketPath = basePath.resolve(bucket);
            if (Files.exists(bucketPath)) {
                usedBytes = calculateDirectorySize(bucketPath);
                objectCount = countFiles(bucketPath);
            }
        } else {
            if (Files.exists(basePath)) {
                usedBytes = calculateDirectorySize(basePath);
                objectCount = countFiles(basePath);
            }
        }

        return StorageUsage.builder()
            .usedBytes(usedBytes)
            .totalBytes(-1L) // 本地存储无配额限制
            .usedHumanReadable(formatBytes(usedBytes))
            .totalHumanReadable("无限制")
            .usedPercentage(0.0)
            .objectCount(objectCount)
            .bucketCount(1L)
            .storageType(STORAGE_TYPE)
            .build();
    }

    @Override
    public String[] getSupportedFeatures() {
        return new String[]{"direct_access", "checksum_verification"};
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String getPresignedUrl(String bucket, String key, int expirationMinutes) {
        // 本地存储不支持预签名URL
        return null;
    }

    /**
     * 计算文件的MD5校验和
     */
    private String calculateMD5(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(md.digest());
    }

    /**
     * 计算文件的SHA256校验和
     */
    private String calculateSHA256(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(md.digest());
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
     * 计算目录大小
     */
    private long calculateDirectorySize(Path path) throws IOException {
        long size = 0;
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    size += calculateDirectorySize(entry);
                }
            }
        } else if (Files.isRegularFile(path)) {
            size = Files.size(path);
        }
        return size;
    }

    /**
     * 统计文件数量
     */
    private long countFiles(Path path) throws IOException {
        long count = 0;
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    count += countFiles(entry);
                }
            }
        } else if (Files.isRegularFile(path)) {
            count = 1;
        }
        return count;
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
}
