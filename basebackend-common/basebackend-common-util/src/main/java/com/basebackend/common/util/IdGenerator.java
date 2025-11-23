package com.basebackend.common.util;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成器工具类
 * <p>
 * 提供多种 ID 生成策略，包括 UUID、雪花算法等。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // UUID
 * String uuid = IdGenerator.uuid();
 * String simpleUuid = IdGenerator.simpleUuid();
 *
 * // 雪花算法
 * long snowflakeId = IdGenerator.snowflakeId();
 *
 * // 时间戳ID
 * String timestampId = IdGenerator.timestampId();
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class IdGenerator {

    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== UUID 生成 ==========

    /**
     * 生成标准 UUID（带横杠）
     *
     * @return UUID 字符串（如：550e8400-e29b-41d4-a716-446655440000）
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成简化 UUID（不带横杠）
     *
     * @return 32 位 UUID 字符串（如：550e8400e29b41d4a716446655440000）
     */
    public static String simpleUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成快速 UUID（使用 ThreadLocalRandom，性能更高）
     * <p>
     * 注意：此方法生成的 UUID 安全性较低，适用于非安全场景。
     * </p>
     *
     * @return 32 位 UUID 字符串
     */
    public static String fastUuid() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
    }

    // ========== 雪花算法 ==========

    /**
     * 雪花算法 ID 生成器
     */
    private static final SnowflakeIdWorker SNOWFLAKE = new SnowflakeIdWorker();

    /**
     * 生成雪花算法 ID
     * <p>
     * 生成全局唯一、时间有序的 64 位 ID。
     * </p>
     *
     * @return 雪花算法 ID
     */
    public static long snowflakeId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 生成雪花算法 ID（字符串形式）
     *
     * @return 雪花算法 ID 字符串
     */
    public static String snowflakeIdStr() {
        return String.valueOf(SNOWFLAKE.nextId());
    }

    // ========== 时间戳 ID ==========

    /** 序列号计数器 */
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    /**
     * 生成时间戳 ID
     * <p>
     * 格式：yyyyMMddHHmmssSSS + 4位序列号
     * </p>
     *
     * @return 21 位时间戳 ID
     */
    public static String timestampId() {
        long timestamp = System.currentTimeMillis();
        long seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("%tY%<tm%<td%<tH%<tM%<tS%<tL%04d", timestamp, seq);
    }

    /**
     * 生成紧凑时间戳 ID
     * <p>
     * 格式：yyyyMMddHHmmss + 6位随机数
     * </p>
     *
     * @return 20 位时间戳 ID
     */
    public static String compactTimestampId() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000000);
        return String.format("%tY%<tm%<td%<tH%<tM%<tS%06d", timestamp, random);
    }

    // ========== 随机字符串 ==========

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final char[] ALPHANUMERIC_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * 生成随机十六进制字符串
     *
     * @param length 字符串长度
     * @return 随机十六进制字符串
     */
    public static String randomHex(int length) {
        if (length <= 0) {
            return "";
        }
        SecureRandom random = new SecureRandom();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = HEX_CHARS[random.nextInt(HEX_CHARS.length)];
        }
        return new String(chars);
    }

    /**
     * 生成随机字母数字字符串
     *
     * @param length 字符串长度
     * @return 随机字母数字字符串
     */
    public static String randomAlphanumeric(int length) {
        if (length <= 0) {
            return "";
        }
        SecureRandom random = new SecureRandom();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHANUMERIC_CHARS[random.nextInt(ALPHANUMERIC_CHARS.length)];
        }
        return new String(chars);
    }

    /**
     * 生成随机数字字符串
     *
     * @param length 字符串长度
     * @return 随机数字字符串
     */
    public static String randomNumeric(int length) {
        if (length <= 0) {
            return "";
        }
        SecureRandom random = new SecureRandom();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ('0' + random.nextInt(10));
        }
        return new String(chars);
    }

    // ========== 雪花算法内部实现 ==========

    /**
     * 雪花算法 ID 生成器
     * <p>
     * 结构（64位）：
     * </p>
     * <ul>
     *   <li>1 bit：符号位（固定为0）</li>
     *   <li>41 bits：时间戳（毫秒级，可使用约69年）</li>
     *   <li>10 bits：机器ID（最多支持1024台机器）</li>
     *   <li>12 bits：序列号（每毫秒最多生成4096个ID）</li>
     * </ul>
     */
    private static class SnowflakeIdWorker {

        /** 开始时间戳（2024-01-01 00:00:00） */
        private static final long EPOCH = 1704067200000L;

        /** 机器ID所占位数 */
        private static final long WORKER_ID_BITS = 10L;

        /** 序列号所占位数 */
        private static final long SEQUENCE_BITS = 12L;

        /** 机器ID最大值 */
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

        /** 序列号最大值 */
        private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

        /** 机器ID左移位数 */
        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

        /** 时间戳左移位数 */
        private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

        /** 机器ID */
        private final long workerId;

        /** 序列号 */
        private long sequence = 0L;

        /** 上次生成ID的时间戳 */
        private long lastTimestamp = -1L;

        /**
         * 构造函数（自动生成机器ID）
         */
        public SnowflakeIdWorker() {
            // 使用 MAC 地址和进程 ID 生成机器 ID
            this.workerId = generateWorkerId();
        }

        /**
         * 构造函数
         *
         * @param workerId 机器ID
         */
        public SnowflakeIdWorker(long workerId) {
            if (workerId < 0 || workerId > MAX_WORKER_ID) {
                throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
            }
            this.workerId = workerId;
        }

        /**
         * 生成下一个 ID
         *
         * @return 雪花算法 ID
         */
        public synchronized long nextId() {
            long timestamp = System.currentTimeMillis();

            // 时钟回拨检测
            if (timestamp < lastTimestamp) {
                long offset = lastTimestamp - timestamp;
                if (offset <= 5) {
                    // 允许 5ms 内的时钟回拨，等待追上
                    try {
                        Thread.sleep(offset << 1);
                        timestamp = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Clock moved backwards. Refusing to generate id", e);
                    }
                }
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + offset + " milliseconds");
                }
            }

            // 同一毫秒内，序列号自增
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
                // 序列号溢出，等待下一毫秒
                if (sequence == 0) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒，序列号重置
                sequence = ThreadLocalRandom.current().nextLong(0, 3);
            }

            lastTimestamp = timestamp;

            return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }

        /**
         * 等待下一毫秒
         */
        private long tilNextMillis(long lastTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }

        /**
         * 自动生成机器ID
         */
        private long generateWorkerId() {
            try {
                // 使用进程 ID 和随机数生成
                long processId = ProcessHandle.current().pid();
                int random = ThreadLocalRandom.current().nextInt(100);
                return (processId + random) % (MAX_WORKER_ID + 1);
            } catch (Exception e) {
                return ThreadLocalRandom.current().nextLong(0, MAX_WORKER_ID + 1);
            }
        }
    }
}
