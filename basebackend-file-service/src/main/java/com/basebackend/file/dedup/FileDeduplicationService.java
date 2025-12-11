package com.basebackend.file.dedup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 文件去重服务
 * <p>
 * 基于文件内容哈希（SHA-256）实现文件去重存储。
 * 相同内容的文件只存储一次，节省存储空间。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeduplicationService {

    private final StringRedisTemplate redisTemplate;

    /** 去重信息缓存键前缀 */
    private static final String DEDUP_KEY_PREFIX = "file_dedup:";

    /** 哈希算法 */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** 缓存过期时间（天） */
    private static final long CACHE_EXPIRE_DAYS = 365;

    /**
     * 计算文件内容哈希
     *
     * @param inputStream 文件输入流
     * @return SHA-256哈希值（十六进制字符串）
     */
    public String calculateHash(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的哈希算法: " + HASH_ALGORITHM, e);
        }
    }

    /**
     * 计算字节数组的哈希
     *
     * @param data 文件数据
     * @return SHA-256哈希值
     */
    public String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return bytesToHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的哈希算法: " + HASH_ALGORITHM, e);
        }
    }

    /**
     * 检查文件是否已存在（去重检测）
     *
     * @param contentHash 文件内容哈希
     * @return 如果存在返回去重信息，否则返回null
     */
    public DeduplicationInfo findByHash(String contentHash) {
        try {
            String key = DEDUP_KEY_PREFIX + contentHash;
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (data.isEmpty()) {
                return null;
            }

            DeduplicationInfo info = new DeduplicationInfo();
            info.setContentHash(contentHash);
            info.setFileSize(Long.parseLong((String) data.get("fileSize")));
            info.setStoragePath((String) data.get("storagePath"));
            info.setReferenceCount(Integer.parseInt((String) data.get("referenceCount")));
            info.setFirstUploadTime(Long.parseLong((String) data.get("firstUploadTime")));
            info.setLastReferenceTime(Long.parseLong((String) data.get("lastReferenceTime")));

            return info;
        } catch (Exception e) {
            log.error("查询去重信息失败: contentHash={}, error={}", contentHash, e.getMessage());
            return null;
        }
    }

    /**
     * 注册新文件（第一次上传）
     *
     * @param contentHash 文件内容哈希
     * @param fileSize    文件大小
     * @param storagePath 存储路径
     * @return 去重信息
     */
    public DeduplicationInfo registerNewFile(String contentHash, long fileSize, String storagePath) {
        DeduplicationInfo info = DeduplicationInfo.newFile(contentHash, fileSize, storagePath);
        saveDeduplicationInfo(info);
        log.info("注册新文件: contentHash={}, size={}, path={}", contentHash, fileSize, storagePath);
        return info;
    }

    /**
     * 增加文件引用（重复文件）
     *
     * @param contentHash 文件内容哈希
     * @return 更新后的去重信息
     */
    public DeduplicationInfo incrementReference(String contentHash) {
        DeduplicationInfo existing = findByHash(contentHash);
        if (existing == null) {
            throw new IllegalStateException("文件不存在: " + contentHash);
        }

        DeduplicationInfo updated = DeduplicationInfo.duplicateFile(existing);
        saveDeduplicationInfo(updated);

        log.info("增加文件引用: contentHash={}, refCount={}", contentHash, updated.getReferenceCount());
        return updated;
    }

    /**
     * 减少文件引用（删除文件时）
     *
     * @param contentHash 文件内容哈希
     * @return 剩余引用计数，如果为0表示可以删除实际文件
     */
    public int decrementReference(String contentHash) {
        DeduplicationInfo existing = findByHash(contentHash);
        if (existing == null) {
            return 0;
        }

        int newCount = existing.getReferenceCount() - 1;

        if (newCount <= 0) {
            // 删除去重信息
            String key = DEDUP_KEY_PREFIX + contentHash;
            redisTemplate.delete(key);
            log.info("删除去重信息（引用归零）: contentHash={}", contentHash);
            return 0;
        } else {
            // 更新引用计数
            existing.setReferenceCount(newCount);
            saveDeduplicationInfo(existing);
            log.info("减少文件引用: contentHash={}, refCount={}", contentHash, newCount);
            return newCount;
        }
    }

    /**
     * 处理上传（自动去重）
     *
     * @param data     文件数据
     * @param uploader 上传处理函数（只有新文件才会调用）
     * @return 去重信息
     */
    public DeduplicationInfo handleUpload(byte[] data, FileUploader uploader) throws IOException {
        String contentHash = calculateHash(data);

        // 检查是否已存在
        DeduplicationInfo existing = findByHash(contentHash);
        if (existing != null) {
            // 文件已存在，增加引用
            log.info("检测到重复文件，跳过上传: contentHash={}, savedBytes={}", contentHash, data.length);
            return incrementReference(contentHash);
        }

        // 新文件，执行上传
        String storagePath = uploader.upload(data);
        return registerNewFile(contentHash, data.length, storagePath);
    }

    /**
     * 获取去重统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        // 这里简化实现，生产环境可以使用更复杂的统计
        Map<String, Object> stats = new HashMap<>();
        stats.put("algorithm", HASH_ALGORITHM);
        stats.put("cacheExpireDays", CACHE_EXPIRE_DAYS);
        return stats;
    }

    // ========== 私有方法 ==========

    private void saveDeduplicationInfo(DeduplicationInfo info) {
        String key = DEDUP_KEY_PREFIX + info.getContentHash();
        Map<String, String> data = new HashMap<>();
        data.put("fileSize", String.valueOf(info.getFileSize()));
        data.put("storagePath", info.getStoragePath());
        data.put("referenceCount", String.valueOf(info.getReferenceCount()));
        data.put("firstUploadTime", String.valueOf(info.getFirstUploadTime()));
        data.put("lastReferenceTime", String.valueOf(info.getLastReferenceTime()));

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 文件上传函数接口
     */
    @FunctionalInterface
    public interface FileUploader {
        /**
         * 执行上传
         *
         * @param data 文件数据
         * @return 存储路径
         */
        String upload(byte[] data) throws IOException;
    }
}
