package com.basebackend.logging.masking;

import java.util.concurrent.atomic.LongAdder;

/**
 * 脱敏系统监控指标
 *
 * 使用LongAdder保证高并发场景下的性能。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class MaskingMetrics {

    /**
     * 已脱敏次数
     */
    private final LongAdder masked = new LongAdder();

    /**
     * 脱敏请求总数
     */
    private final LongAdder hits = new LongAdder();

    /**
     * 脱敏总耗时（纳秒）
     */
    private final LongAdder timeNanos = new LongAdder();

    /**
     * 记录脱敏操作
     *
     * @param nanos 耗时（纳秒）
     * @param applied 是否进行了脱敏
     */
    public void record(long nanos, boolean applied) {
        timeNanos.add(nanos);
        if (applied) {
            masked.increment();
        }
        hits.increment();
    }

    /**
     * 获取脱敏次数
     */
    public long getMasked() {
        return masked.sum();
    }

    /**
     * 获取总请求数
     */
    public long getHits() {
        return hits.sum();
    }

    /**
     * 获取平均脱敏时间（微秒）
     */
    public long getAvgMicros() {
        long h = hits.sum();
        if (h == 0) {
            return 0;
        }
        return timeNanos.sum() / h / 1_000;
    }

    /**
     * 获取脱敏率（百分比）
     */
    public double getMaskingRate() {
        long h = hits.sum();
        if (h == 0) {
            return 0.0;
        }
        return (double) masked.sum() / h * 100.0;
    }

    /**
     * 获取性能评级
     */
    public PerformanceGrade getPerformanceGrade() {
        long avgMicros = getAvgMicros();
        if (avgMicros < 1000) {
            return PerformanceGrade.EXCELLENT;
        } else if (avgMicros < 3000) {
            return PerformanceGrade.GOOD;
        } else if (avgMicros < 5000) {
            return PerformanceGrade.FAIR;
        } else {
            return PerformanceGrade.POOR;
        }
    }

    /**
     * 性能等级枚举
     */
    public enum PerformanceGrade {
        EXCELLENT("优秀", "<1ms"),
        GOOD("良好", "1-3ms"),
        FAIR("一般", "3-5ms"),
        POOR("较差", ">5ms");

        private final String label;
        private final String description;

        PerformanceGrade(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取统计摘要
     */
    public String getSummary() {
        return String.format(
                "脱敏统计 - 总请求: %d, 已脱敏: %d, 平均耗时: %dμs, 脱敏率: %.2f%%, 性能等级: %s",
                hits.sum(), masked.sum(), getAvgMicros(),
                getMaskingRate(), getPerformanceGrade().getLabel());
    }
}
