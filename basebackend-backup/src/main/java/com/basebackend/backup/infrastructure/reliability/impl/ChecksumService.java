package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.Checksum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 校验服务
 * <p>
 * 提供文件完整性校验功能，支持MD5和SHA256算法。
 * 用于验证备份文件在传输和存储过程中的完整性。
 *
 * @author BaseBackend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChecksumService {

    private final BackupProperties backupProperties;

    /**
     * 计算文件的校验和
     *
     * @param filePath 文件路径
     * @return 校验和结果
     * @throws Exception 计算失败时抛出异常
     */
    public Checksum computeChecksum(Path filePath) throws Exception {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        long fileSize = Files.size(filePath);
        log.debug("开始计算文件校验和: {}, 大小: {} bytes", filePath, fileSize);

        // 获取启用的算法
        String[] algorithms = backupProperties.getChecksum().getAlgorithms().toArray(new String[0]);

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

        log.info("文件校验和计算完成: {}, MD5: {}, SHA256: {}",
            filePath, md5, sha256);

        return checksum;
    }

    /**
     * 计算输入流的校验和
     *
     * @param inputStream 输入流
     * @param fileSize 文件大小（可选）
     * @return 校验和结果
     * @throws Exception 计算失败时抛出异常
     */
    public Checksum computeChecksum(InputStream inputStream, Long fileSize) throws Exception {
        log.debug("开始计算输入流校验和");

        String[] algorithms = backupProperties.getChecksum().getAlgorithms().toArray(new String[0]);

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
    }

    /**
     * 验证文件校验和
     *
     * @param filePath 文件路径
     * @param expectedMd5 期望的MD5（可选）
     * @param expectedSha256 期望的SHA256（可选）
     * @return 验证结果
     * @throws Exception 验证失败时抛出异常
     */
    public boolean verifyChecksum(Path filePath, String expectedMd5, String expectedSha256) throws Exception {
        Checksum actualChecksum = computeChecksum(filePath);
        return actualChecksum.verify(expectedMd5, expectedSha256);
    }

    /**
     * 计算MD5校验和
     */
    private String computeMD5(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(filePath)) {
            updateFromInputStream(md, is);
        }
        return bytesToHex(md.digest());
    }

    /**
     * 计算SHA256校验和
     */
    private String computeSHA256(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(filePath)) {
            updateFromInputStream(md, is);
        }
        return bytesToHex(md.digest());
    }

    /**
     * 从输入流计算MD5
     */
    private String computeMD5(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        updateFromInputStream(md, inputStream);
        return bytesToHex(md.digest());
    }

    /**
     * 从输入流计算SHA256
     */
    private String computeSHA256(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        updateFromInputStream(md, inputStream);
        return bytesToHex(md.digest());
    }

    /**
     * 从输入流更新所有MessageDigest
     * 单次遍历计算多个digest
     */
    private void updateAllDigests(InputStream is, MessageDigest... digests) throws Exception {
        // 过滤掉null的digest
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
            // 将数据更新到所有digest
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
