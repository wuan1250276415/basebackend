package com.basebackend.logging.audit.crypto;

import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 哈希链计算器
 *
 * 使用 SHA-256 算法计算审计日志的哈希链，
 * 保证审计日志的完整性和不可篡改性。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class HashChainCalculator {

    private static final String DEFAULT_ALGORITHM = "SHA-256";
    private final String algorithm;
    private final ObjectMapper objectMapper;

    public HashChainCalculator() {
        this(DEFAULT_ALGORITHM);
    }

    public HashChainCalculator(String algorithm) {
        this(algorithm, new ObjectMapper());
    }

    public HashChainCalculator(String algorithm, ObjectMapper objectMapper) {
        this.algorithm = algorithm;
        this.objectMapper = objectMapper;
    }

    /**
     * 计算审计日志条目的哈希值
     *
     * @param entry   审计日志条目
     * @param prevHash 前一个日志的哈希值（可选）
     * @return 当前日志的哈希值
     */
    public String computeHash(AuditLogEntry entry, String prevHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            // 添加前一个哈希值（如果有）
            if (prevHash != null && !prevHash.isEmpty()) {
                digest.update(prevHash.getBytes(StandardCharsets.UTF_8));
            } else {
                // 种子值：空字节
                digest.update(new byte[0]);
            }

            // 添加当前条目的 JSON 序列化内容
            byte[] jsonBytes = objectMapper.writeValueAsString(entry).getBytes(StandardCharsets.UTF_8);
            digest.update(jsonBytes);

            // 返回十六进制编码的哈希值
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            log.error("哈希链计算失败", e);
            throw new RuntimeException("哈希链计算失败", e);
        }
    }

    /**
     * 验证哈希链的完整性
     *
     * @param entries 审计日志条目列表（按时间排序）
     * @return 验证结果：true 表示完整性校验通过
     */
    public boolean verifyChain(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return true;
        }

        String prevHash = null;
        int index = 0;

        try {
            for (AuditLogEntry entry : entries) {
                index++;

                // 计算期望的哈希值
                String expectedHash = computeHash(entry, prevHash);

                // 验证当前条目的哈希值
                String actualHash = entry.getEntryHash();
                if (actualHash == null || actualHash.isEmpty()) {
                    log.warn("审计日志条目 [{}] 缺少哈希值", index);
                    return false;
                }

                if (!expectedHash.equals(actualHash)) {
                    log.error("审计日志条目 [{}] 哈希值不匹配", index);
                    log.error("期望: {}", expectedHash);
                    log.error("实际: {}", actualHash);
                    return false;
                }

                // 验证前一个哈希值
                String actualPrevHash = entry.getPrevHash();
                if (actualPrevHash != null && prevHash != null) {
                    if (!actualPrevHash.equals(prevHash)) {
                        log.error("审计日志条目 [{}] 前置哈希值不匹配", index);
                        return false;
                    }
                }

                // 更新前一个哈希值
                prevHash = expectedHash;
            }

            log.info("哈希链完整性验证通过，共验证 {} 个条目", entries.size());
            return true;
        } catch (Exception e) {
            log.error("哈希链验证过程中发生异常", e);
            return false;
        }
    }

    /**
     * 验证单个审计日志条目的哈希值
     *
     * @param entry     审计日志条目
     * @param prevHash 前一个日志的哈希值
     * @return 验证结果
     */
    public boolean verifyEntry(AuditLogEntry entry, String prevHash) {
        if (entry == null) {
            return false;
        }

        try {
            String expectedHash = computeHash(entry, prevHash);
            String actualHash = entry.getEntryHash();

            return expectedHash.equals(actualHash);
        } catch (Exception e) {
            log.error("哈希链条目验证失败", e);
            return false;
        }
    }

    /**
     * 计算哈希链的根哈希值
     *
     * @param entries 审计日志条目列表
     * @return 根哈希值（最后一个条目的哈希值）
     */
    public String computeRootHash(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        String prevHash = null;
        for (AuditLogEntry entry : entries) {
            prevHash = computeHash(entry, prevHash);
        }

        return prevHash;
    }

    /**
     * 检测哈希链中可能的篡改
     *
     * @param entries 审计日志条目列表
     * @return 检测结果信息
     */
    public String detectTampering(List<AuditLogEntry> entries) {
        StringBuilder report = new StringBuilder();
        report.append("哈希链篡改检测报告\n");
        report.append("=" .repeat(50)).append("\n");

        if (verifyChain(entries)) {
            report.append("✓ 完整性校验通过，未发现篡改\n");
        } else {
            report.append("✗ 发现篡改或异常\n");

            // 尝试找到第一个问题点
            String prevHash = null;
            int problemIndex = -1;

            for (int i = 0; i < entries.size(); i++) {
                AuditLogEntry entry = entries.get(i);
                String expectedHash = computeHash(entry, prevHash);

                if (!expectedHash.equals(entry.getEntryHash())) {
                    problemIndex = i + 1;
                    report.append("问题出现在第 ").append(problemIndex).append(" 个条目\n");
                    report.append("  条目ID: ").append(entry.getId()).append("\n");
                    report.append("  时间: ").append(entry.getTimestamp()).append("\n");
                    report.append("  用户: ").append(entry.getUserId()).append("\n");
                    report.append("  期望哈希: ").append(expectedHash).append("\n");
                    report.append("  实际哈希: ").append(entry.getEntryHash()).append("\n");
                    break;
                }

                prevHash = expectedHash;
            }
        }

        return report.toString();
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
