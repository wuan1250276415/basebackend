package com.basebackend.storage.reliability.impl;

import com.basebackend.storage.config.StorageProperties;
import com.basebackend.storage.model.Checksum;
import com.basebackend.storage.reliability.ChecksumProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 校验服务实现
 * <p>
 * 提供文件完整性校验功能，支持MD5和SHA256算法。
 * 用于验证文件在传输和存储过程中的完整性。
 *
 * @author BaseBackend
 */
@Slf4j
public class ChecksumServiceImpl implements ChecksumProvider {

    private final StorageProperties.ChecksumConfig checksumConfig;

    public ChecksumServiceImpl(StorageProperties properties) {
        this.checksumConfig = properties.getChecksum();
    }

    @Override
    public Checksum computeChecksum(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        try {
            long fileSize = Files.size(filePath);
            log.debug("开始计算文件校验和: {}, 大小: {} bytes", filePath, fileSize);

            String[] algorithms = checksumConfig.getAlgorithms();
            String md5 = null;
            String sha256 = null;

            for (String algorithm : algorithms) {
                switch (algorithm.toUpperCase()) {
                    case "MD5":
                        md5 = computeMD5(filePath);
                        break;
                    case "SHA256":
                        sha256 = computeSHA256(filePath);
                        break;
                    default:
                        log.warn("不支持的校验算法: {}", algorithm);
                }
            }

            Checksum checksum = Checksum.builder()
                    .md5(md5)
                    .sha256(sha256)
                    .fileSize(fileSize)
                    .algorithms(algorithms)
                    .calculatedAt(LocalDateTime.now())
                    .build();

            log.info("文件校验和计算完成: {}, MD5: {}, SHA256: {}", filePath, md5, sha256);
            return checksum;
            
        } catch (Exception e) {
            log.error("计算文件校验和失败: {}", filePath, e);
            throw new RuntimeException("计算校验和失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Checksum computeChecksum(InputStream inputStream, Long fileSize) {
        log.debug("开始计算输入流校验和");

        try {
            String[] algorithms = checksumConfig.getAlgorithms();

            // 初始化所有需要的MessageDigest
            MessageDigest md5Digest = null;
            MessageDigest sha256Digest = null;

            for (String algorithm : algorithms) {
                switch (algorithm.toUpperCase()) {
                    case "MD5":
                        md5Digest = MessageDigest.getInstance("MD5");
                        break;
                    case "SHA256":
                        sha256Digest = MessageDigest.getInstance("SHA-256");
                        break;
                    default:
                        log.warn("不支持的校验算法: {}", algorithm);
                }
            }

            // 单次遍历计算所有digest
            updateAllDigests(inputStream, md5Digest, sha256Digest);

            // 生成校验和字符串
            String md5 = md5Digest != null ? bytesToHex(md5Digest.digest()) : null;
            String sha256 = sha256Digest != null ? bytesToHex(sha256Digest.digest()) : null;

            Checksum checksum = Checksum.builder()
                    .md5(md5)
                    .sha256(sha256)
                    .fileSize(fileSize)
                    .algorithms(algorithms)
                    .calculatedAt(LocalDateTime.now())
                    .build();

            log.info("输入流校验和计算完成, MD5: {}, SHA256: {}", md5, sha256);
            return checksum;
            
        } catch (Exception e) {
            log.error("计算输入流校验和失败", e);
            throw new RuntimeException("计算校验和失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyChecksum(Path filePath, String expectedMd5, String expectedSha256) {
        Checksum actualChecksum = computeChecksum(filePath);
        return actualChecksum.verify(expectedMd5, expectedSha256);
    }

    @Override
    public String computeMD5(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(filePath)) {
                updateFromInputStream(md, is);
            }
            return bytesToHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算MD5失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String computeSHA256(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(filePath)) {
                updateFromInputStream(md, is);
            }
            return bytesToHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算SHA256失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String computeMD5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            updateFromInputStream(md, inputStream);
            return bytesToHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算MD5失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String computeSHA256(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            updateFromInputStream(md, inputStream);
            return bytesToHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算SHA256失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从输入流更新所有MessageDigest（单次遍历计算多个digest）
     */
    private void updateAllDigests(InputStream is, MessageDigest... digests) throws Exception {
        MessageDigest[] validDigests = Arrays.stream(digests)
                .filter(d -> d != null)
                .toArray(MessageDigest[]::new);

        if (validDigests.length == 0) {
            log.warn("没有有效的MessageDigest需要更新");
            return;
        }

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            for (MessageDigest digest : validDigests) {
                digest.update(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 从输入流更新MessageDigest
     */
    private void updateFromInputStream(MessageDigest md, InputStream is) throws Exception {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
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
}
