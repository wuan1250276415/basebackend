package com.basebackend.observability.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间窗口计算工具类
 */
public class TimeWindowCalculator {

    /**
     * 生成时间窗口
     */
    public static List<TimeWindow> generateTimeWindows(
            LocalDateTime start, 
            LocalDateTime end, 
            int windowSizeMinutes) {
        
        List<TimeWindow> windows = new ArrayList<>();
        
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            LocalDateTime windowEnd = current.plusMinutes(windowSizeMinutes);
            if (windowEnd.isAfter(end)) {
                windowEnd = end;
            }
            
            windows.add(new TimeWindow(current, windowEnd));
            current = windowEnd;
        }
        
        return windows;
    }

    /**
     * 生成小时窗口
     */
    public static List<TimeWindow> generateHourlyWindows(int hours) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);
        return generateTimeWindows(start, end, 60);
    }

    /**
     * 生成分钟窗口
     */
    public static List<TimeWindow> generateMinuteWindows(int minutes, int windowSizeMinutes) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(minutes);
        return generateTimeWindows(start, end, windowSizeMinutes);
    }

    /**
     * 计算两个时间的差值（毫秒）
     */
    public static long getDurationMillis(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MILLIS.between(start, end);
    }

    /**
     * 时间窗口类
     */
    public static class TimeWindow {
        private final LocalDateTime start;
        private final LocalDateTime end;
        
        public TimeWindow(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
        
        public LocalDateTime getStart() {
            return start;
        }
        
        public LocalDateTime getEnd() {
            return end;
        }
        
        public long getDurationMillis() {
            return ChronoUnit.MILLIS.between(start, end);
        }
        
        public boolean contains(LocalDateTime time) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        
        @Override
        public String toString() {
            return String.format("[%s -> %s]", start, end);
        }
    }
}
