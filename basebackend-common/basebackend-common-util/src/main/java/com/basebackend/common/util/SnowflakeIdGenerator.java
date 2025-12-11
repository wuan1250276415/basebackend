package com.basebackend.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法ID生成器（优化版本）
 * <p>
 * 使用 AtomicLong 替代 synchronized，提高高并发场景下的性能。
 * 支持手动配置 workerId，也支持自动生成。
 * </p>
 *
 * <h3>结构（64位）：</h3>
 * <ul>
 * <li>1 bit：符号位（固定为0）</li>
 * <li>41 bits：时间戳（毫秒级，可使用约69年）</li>
 * <li>10 bits：机器ID（最多支持1024台机器）</li>
 * <li>12 bits：序列号（每毫秒最多生成4096个ID）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 方式1：使用默认实例
 * long id = SnowflakeIdGenerator.nextId();
 *
 * // 方式2：创建自定义实例
 * SnowflakeIdGenerator generator = new SnowflakeIdGenerator(5);
 * long id = generator.generateId();
 *
 * // 方式3：配置化
 * SnowflakeIdGenerator.setWorkerId(10);
 * long id = SnowflakeIdGenerator.nextId();
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class SnowflakeIdGenerator {

    // ========== 常量定义 ==========

    /** 开始时间戳（2024-01-01 00:00:00） */
    private static final long EPOCH = 1704067200000L;

    /** 机器ID所占位数 */
    private static final int WORKER_ID_BITS = 10;

    /** 序列号所占位数 */
    private static final int SEQUENCE_BITS = 12;

    /** 机器ID最大值 (1023) */
    public static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 序列号最大值 (4095) */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 机器ID左移位数 */
    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数 */
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间回拨容忍阈值（毫秒） */
    private static final long CLOCK_DRIFT_TOLERANCE = 5L;

    // ========== 实例变量 ==========

    /** 机器ID */
    private final long workerId;

    /**
     * 组合状态（高41位存储时间戳，低12位存储序列号）
     * 使用 AtomicLong 实现无锁并发
     */
    private final AtomicLong state;

    // ========== 静态默认实例 ==========

    /** 默认实例 */
    private static volatile SnowflakeIdGenerator defaultInstance;

    /** 配置的 workerId */
    private static volatile Long configuredWorkerId;

    // ========== 构造函数 ==========

    /**
     * 使用自动生成的机器ID构造
     */
    public SnowflakeIdGenerator() {
        this(generateAutoWorkerId());
    }

    /**
     * 使用指定的机器ID构造
     *
     * @param workerId 机器ID (0 - 1023)
     */
    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("Worker ID must be between 0 and %d, but got: %d", MAX_WORKER_ID, workerId));
        }
        this.workerId = workerId;
        this.state = new AtomicLong(0L);
        log.info("SnowflakeIdGenerator initialized with workerId: {}", workerId);
    }

    // ========== 静态方法 ==========

    /**
     * 设置默认实例的 workerId
     * <p>
     * 一般在应用启动时通过配置设置。
     * </p>
     *
     * @param workerId 机器ID (0 - 1023)
     */
    public static synchronized void setWorkerId(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("Worker ID must be between 0 and %d, but got: %d", MAX_WORKER_ID, workerId));
        }
        configuredWorkerId = workerId;
        defaultInstance = new SnowflakeIdGenerator(workerId);
        log.info("SnowflakeIdGenerator default instance configured with workerId: {}", workerId);
    }

    /**
     * 获取默认实例
     */
    private static SnowflakeIdGenerator getDefaultInstance() {
        if (defaultInstance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (defaultInstance == null) {
                    long workerId = configuredWorkerId != null ? configuredWorkerId : generateAutoWorkerId();
                    defaultInstance = new SnowflakeIdGenerator(workerId);
                }
            }
        }
        return defaultInstance;
    }

    /**
     * 使用默认实例生成ID
     *
     * @return 雪花算法ID
     */
    public static long nextId() {
        return getDefaultInstance().generateId();
    }

    /**
     * 使用默认实例生成ID（字符串形式）
     *
     * @return 雪花算法ID字符串
     */
    public static String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 获取当前配置的 workerId
     *
     * @return workerId
     */
    public static long getWorkerId() {
        return getDefaultInstance().workerId;
    }

    // ========== 实例方法 ==========

    /**
     * 生成下一个ID
     * <p>
     * 使用 CAS 操作实现无锁并发，性能优于 synchronized。
     * </p>
     *
     * @return 雪花算法ID
     */
    public long generateId() {
        while (true) {
            long currentState = state.get();
            long lastTimestamp = extractTimestamp(currentState);
            long lastSequence = extractSequence(currentState);

            long currentTimestamp = System.currentTimeMillis();

            // 时钟回拨检测
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= CLOCK_DRIFT_TOLERANCE) {
                    // 短暂回拨，等待追上
                    try {
                        Thread.sleep(offset << 1);
                        currentTimestamp = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Clock moved backwards. Refusing to generate id", e);
                    }
                }
                if (currentTimestamp < lastTimestamp) {
                    throw new RuntimeException(
                            "Clock moved backwards. Refusing to generate id for " + offset + " milliseconds");
                }
            }

            long newSequence;
            if (currentTimestamp == lastTimestamp) {
                // 同一毫秒，序列号递增
                newSequence = (lastSequence + 1) & MAX_SEQUENCE;
                if (newSequence == 0) {
                    // 序列号溢出，等待下一毫秒
                    currentTimestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒，序列号重置为随机起始值（避免低位分布不均）
                newSequence = Thread.currentThread().getId() & 0x03L;
            }

            // 构建新状态
            long newState = buildState(currentTimestamp, newSequence);

            // CAS 更新状态
            if (state.compareAndSet(currentState, newState)) {
                return buildId(currentTimestamp, newSequence);
            }
            // CAS 失败，重试
        }
    }

    /**
     * 解析ID，获取生成时间戳
     *
     * @param id 雪花算法ID
     * @return 生成时间戳（毫秒）
     */
    public static long parseTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }

    /**
     * 解析ID，获取机器ID
     *
     * @param id 雪花算法ID
     * @return 机器ID
     */
    public static long parseWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 解析ID，获取序列号
     *
     * @param id 雪花算法ID
     * @return 序列号
     */
    public static long parseSequence(long id) {
        return id & MAX_SEQUENCE;
    }

    // ========== 私有方法 ==========

    /**
     * 从状态中提取时间戳
     */
    private long extractTimestamp(long state) {
        return (state >> SEQUENCE_BITS) + EPOCH;
    }

    /**
     * 从状态中提取序列号
     */
    private long extractSequence(long state) {
        return state & MAX_SEQUENCE;
    }

    /**
     * 构建状态值
     */
    private long buildState(long timestamp, long sequence) {
        return ((timestamp - EPOCH) << SEQUENCE_BITS) | sequence;
    }

    /**
     * 构建ID
     */
    private long buildId(long timestamp, long sequence) {
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            Thread.yield();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 自动生成机器ID
     * <p>
     * 基于进程ID和线程ID生成，在容器环境中可能冲突，
     * 建议生产环境使用配置方式。
     * </p>
     */
    private static long generateAutoWorkerId() {
        try {
            long processId = ProcessHandle.current().pid();
            long threadId = Thread.currentThread().getId();
            long workerId = ((processId << 4) ^ threadId) & MAX_WORKER_ID;
            log.warn(
                    "Auto-generated workerId: {}. Consider configuring explicitly to avoid conflicts in distributed environments.",
                    workerId);
            return workerId;
        } catch (Exception e) {
            long random = (long) (Math.random() * (MAX_WORKER_ID + 1));
            log.warn("Failed to auto-generate workerId, using random value: {}. Error: {}", random, e.getMessage());
            return random;
        }
    }
}
