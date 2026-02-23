package com.basebackend.logging.cost;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 自适应采样过滤器
 *
 * 作为 Logback TurboFilter 运行。当某个服务/来源的日志量超过配置阈值时，
 * 按照采样率随机丢弃低优先级日志事件，防止日志洪峰压垮后端存储。
 *
 * 特性：
 * - WARN/ERROR 级别默认豁免（可配置）
 * - 基于 logger name 的首段作为服务标识
 * - 同时跟踪事件数和字节数两个维度
 *
 * @author basebackend team
 * @since 2025-12-10
 */
public class AdaptiveSamplerFilter extends TurboFilter {

    private final LogVolumeTracker tracker;
    private final long eventThreshold;
    private final long byteThreshold;
    private final double samplingRate;
    private final boolean exemptHighSeverity;

    public AdaptiveSamplerFilter(LogVolumeTracker tracker,
                                  long eventThreshold,
                                  long byteThreshold,
                                  double samplingRate,
                                  boolean exemptHighSeverity) {
        this.tracker = tracker;
        this.eventThreshold = eventThreshold;
        this.byteThreshold = byteThreshold;
        this.samplingRate = samplingRate;
        this.exemptHighSeverity = exemptHighSeverity;
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        if (format == null) {
            return FilterReply.NEUTRAL;
        }

        // WARN/ERROR 豁免
        if (exemptHighSeverity && level != null && level.isGreaterOrEqual(Level.WARN)) {
            trackEvent(logger, format);
            return FilterReply.NEUTRAL;
        }

        String serviceKey = extractServiceKey(logger);
        int estimatedBytes = estimateBytes(format, params);

        // 记录到 tracker
        tracker.record(serviceKey, estimatedBytes);

        // 检查是否超阈值
        long events = tracker.getEventCount(serviceKey);
        long bytes = tracker.getByteCount(serviceKey);

        if (events > eventThreshold || bytes > byteThreshold) {
            // 超阈值：按采样率决定是否保留
            if (ThreadLocalRandom.current().nextDouble() > samplingRate) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }

    private void trackEvent(Logger logger, String format) {
        String serviceKey = extractServiceKey(logger);
        tracker.record(serviceKey, estimateBytes(format, null));
    }

    /**
     * 从 logger name 中提取服务标识（取前 3 段包名）
     */
    String extractServiceKey(Logger logger) {
        if (logger == null || logger.getName() == null) {
            return "unknown";
        }
        String name = logger.getName();
        int count = 0;
        int idx = 0;
        while (count < 3 && idx < name.length()) {
            idx = name.indexOf('.', idx);
            if (idx < 0) {
                return name;
            }
            count++;
            idx++;
        }
        return name.substring(0, idx - 1);
    }

    /**
     * 粗略估算日志消息字节数
     */
    private int estimateBytes(String format, Object[] params) {
        int size = format != null ? format.length() : 0;
        if (params != null) {
            for (Object p : params) {
                size += p != null ? p.toString().length() : 4;
            }
        }
        // 加上元数据（时间戳、级别、logger名等）的估算开销
        return size + 80;
    }
}
