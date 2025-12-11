package com.basebackend.featuretoggle.abtest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * 一致性哈希算法实现
 * <p>
 * 提供多种哈希算法，确保用户分配的稳定性和一致性。
 * 支持权重分配和渐进式发布场景。
 * </p>
 *
 * <h3>算法类型：</h3>
 * <ul>
 *   <li>MURMUR3 - 快速分布式哈希（推荐）</li>
 *   <li>CRC32 - 循环冗余校验，快速但分布性稍差</li>
 *   <li>MD5 - 兼容性好，但速度较慢</li>
 *   <li>SHA1 - 安全性较高，兼容性一般</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashAlgorithm {

    /**
     * MurmurHash3 32位哈希算法
     * <p>
     * 高性能的分布式哈希算法，具有良好的分布性。
     * 适用于大规模用户分配场景。
     * </p>
     *
     * @param key 哈希键
     * @return 哈希值 (0-2^31-1)
     */
    public static long murmur3_32(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }

        final byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        final int length = bytes.length;
        int hash = 0;
        int k1 = 0;

        for (int i = 0; i < length - 3; i += 4) {
            k1 = ((bytes[i] & 0xFF)) |
                 ((bytes[i + 1] & 0xFF) << 8) |
                 ((bytes[i + 2] & 0xFF) << 16) |
                 ((bytes[i + 3] & 0xFF) << 24);
            k1 = (int) Math.multiplyFull(k1, 0xcc9e2d51);
            k1 = Integer.rotateLeft(k1, 15);
            k1 = (int) Math.multiplyFull(k1, 0x1b873593);

            hash ^= k1;
            hash = Integer.rotateLeft(hash, 13);
            hash = (int) (hash * 5 + 0xe6546b64);
        }

        // 处理剩余字节
        int tail = length & 3;
        if (tail > 0) {
            k1 = 0;
            for (int i = 0; i < tail; i++) {
                k1 |= (bytes[length - tail + i] & 0xFF) << (8 * i);
            }
            k1 = (int) Math.multiplyFull(k1, 0xcc9e2d51);
            k1 = Integer.rotateLeft(k1, 15);
            k1 = (int) Math.multiplyFull(k1, 0x1b873593);
            hash ^= k1;
        }

        // 最终混合
        hash ^= length;
        hash ^= hash >>> 16;
        hash = (int) Math.multiplyFull(hash, 0x85ebca6b);
        hash ^= hash >>> 13;
        hash = (int) Math.multiplyFull(hash, 0xc2b2ae35);
        hash ^= hash >>> 16;

        return hash & 0xFFFFFFFFL;
    }

    /**
     * CRC32哈希算法
     * <p>
     * 快速但分布性相对较差，适用于对性能要求极高的场景。
     * </p>
     *
     * @param key 哈希键
     * @return 哈希值 (0-2^31-1)
     */
    public static long crc32(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }
        CRC32 crc32 = new CRC32();
        crc32.update(key.getBytes(StandardCharsets.UTF_8));
        return crc32.getValue();
    }

    /**
     * MD5哈希算法
     * <p>
     * 经典哈希算法，兼容性好但速度相对较慢。
     * </p>
     *
     * @param key 哈希键
     * @return 哈希值 (0-2^31-1)
     */
    public static long md5(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(key.getBytes(StandardCharsets.UTF_8));

            // 取前4字节作为32位哈希值
            long hash = 0;
            for (int i = 0; i < 4 && i < hashBytes.length; i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash & 0xFFFFFFFFL;
        } catch (NoSuchAlgorithmException e) {
            // 不应发生，MD5是标准算法
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * SHA1哈希算法
     * <p>
     * 安全性较高的哈希算法，兼容性一般。
     * </p>
     *
     * @param key 哈希键
     * @return 哈希值 (0-2^31-1)
     */
    public static long sha1(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(key.getBytes(StandardCharsets.UTF_8));

            // 取前4字节作为32位哈希值
            long hash = 0;
            for (int i = 0; i < 4 && i < hashBytes.length; i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash & 0xFFFFFFFFL;
        } catch (NoSuchAlgorithmException e) {
            // 不应发生，SHA-1是标准算法
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    /**
     * 根据百分比计算桶索引
     * <p>
     * 将哈希值映射到0-99的百分比桶中。
     * </p>
     *
     * @param hash 哈希值
     * @return 桶索引 (0-99)
     */
    public static int toPercentileBucket(long hash) {
        return (int) (Math.abs(hash) % 100);
    }

    /**
     * 计算一致性权重值
     * <p>
     * 根据权重百分比和总用户数计算目标用户数。
     * </p>
     *
     * @param hash 哈希值
     * @param totalUsers 总用户数
     * @param weightPercent 权重百分比 (0-100)
     * @return 目标用户数
     */
    public static long calculateWeightedTarget(long hash, long totalUsers, double weightPercent) {
        if (totalUsers <= 0 || weightPercent < 0 || weightPercent > 100) {
            return 0;
        }
        long normalizedHash = Math.abs(hash) % totalUsers;
        return (long) (normalizedHash * weightPercent / 100.0);
    }

    /**
     * 判断用户是否命中权重范围
     * <p>
     * 根据哈希值判断用户是否在指定的权重范围内。
     * </p>
     *
     * @param hash 哈希值
     * @param totalUsers 总用户数
     * @param startPercent 起始百分比 (0-100)
     * @param endPercent 结束百分比 (0-100)
     * @return 是否命中
     */
    public static boolean isInWeightRange(long hash, long totalUsers, double startPercent, double endPercent) {
        if (totalUsers <= 0 || startPercent < 0 || endPercent > 100 || startPercent > endPercent) {
            return false;
        }
        long normalizedHash = Math.abs(hash) % totalUsers;
        double userPercent = (double) normalizedHash / totalUsers * 100;
        return userPercent >= startPercent && userPercent <= endPercent;
    }
}
