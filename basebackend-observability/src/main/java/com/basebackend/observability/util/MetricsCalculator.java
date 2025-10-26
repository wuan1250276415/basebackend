package com.basebackend.observability.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 指标计算工具类
 */
public class MetricsCalculator {

    /**
     * 计算平均值
     */
    public static double average(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        double sum = values.stream()
                .mapToDouble(Number::doubleValue)
                .sum();
        
        return sum / values.size();
    }

    /**
     * 计算中位数
     */
    public static double median(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        List<Double> sorted = new ArrayList<>();
        for (Number num : values) {
            sorted.add(num.doubleValue());
        }
        Collections.sort(sorted);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    /**
     * 计算百分位数
     */
    public static double percentile(List<? extends Number> values, double percentile) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100");
        }
        
        List<Double> sorted = new ArrayList<>();
        for (Number num : values) {
            sorted.add(num.doubleValue());
        }
        Collections.sort(sorted);
        
        int index = (int) Math.ceil(sorted.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        
        return sorted.get(index);
    }

    /**
     * 计算P50
     */
    public static double p50(List<? extends Number> values) {
        return percentile(values, 50);
    }

    /**
     * 计算P95
     */
    public static double p95(List<? extends Number> values) {
        return percentile(values, 95);
    }

    /**
     * 计算P99
     */
    public static double p99(List<? extends Number> values) {
        return percentile(values, 99);
    }

    /**
     * 计算标准差
     */
    public static double standardDeviation(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        double avg = average(values);
        
        double sumSquaredDiff = values.stream()
                .mapToDouble(Number::doubleValue)
                .map(v -> Math.pow(v - avg, 2))
                .sum();
        
        return Math.sqrt(sumSquaredDiff / values.size());
    }

    /**
     * 计算最小值
     */
    public static double min(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .min()
                .orElse(0.0);
    }

    /**
     * 计算最大值
     */
    public static double max(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .max()
                .orElse(0.0);
    }

    /**
     * 计算总和
     */
    public static double sum(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .sum();
    }

    /**
     * 计算变化率
     */
    public static double changeRate(double oldValue, double newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        
        return ((newValue - oldValue) / oldValue) * 100.0;
    }

    /**
     * 格式化字节大小
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 格式化时间（毫秒）
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60000) {
            return String.format("%.2f s", millis / 1000.0);
        } else if (millis < 3600000) {
            return String.format("%.2f min", millis / 60000.0);
        } else {
            return String.format("%.2f h", millis / 3600000.0);
        }
    }

    /**
     * 计算QPS
     */
    public static double calculateQps(long totalRequests, long durationMillis) {
        if (durationMillis == 0) {
            return 0.0;
        }
        
        return (double) totalRequests / (durationMillis / 1000.0);
    }

    /**
     * 计算错误率
     */
    public static double calculateErrorRate(long errorCount, long totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        
        return ((double) errorCount / totalCount) * 100.0;
    }
}
