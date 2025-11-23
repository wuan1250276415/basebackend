package com.basebackend.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 日期时间工具类
 * <p>
 * 基于 Java 8+ 时间 API，提供常用的日期时间操作方法。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 格式化
 * String dateStr = DateUtils.format(LocalDateTime.now());
 * String customStr = DateUtils.format(LocalDateTime.now(), "yyyy/MM/dd");
 *
 * // 解析
 * LocalDateTime dateTime = DateUtils.parseDateTime("2024-01-01 12:00:00");
 * LocalDate date = DateUtils.parseDate("2024-01-01");
 *
 * // 计算
 * long days = DateUtils.daysBetween(start, end);
 * LocalDateTime startOfDay = DateUtils.startOfDay(LocalDateTime.now());
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class DateUtils {

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== 常用格式 ==========

    /** 标准日期时间格式 */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /** 标准日期格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    /** 标准时间格式 */
    public static final String TIME_PATTERN = "HH:mm:ss";
    /** 紧凑日期时间格式 */
    public static final String COMPACT_DATETIME_PATTERN = "yyyyMMddHHmmss";
    /** 紧凑日期格式 */
    public static final String COMPACT_DATE_PATTERN = "yyyyMMdd";

    /** 标准日期时间格式化器 */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    /** 标准日期格式化器 */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    /** 标准时间格式化器 */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    // ========== 格式化方法 ==========

    /**
     * 格式化日期时间（使用默认格式 yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTime 日期时间
     * @return 格式化字符串，如果参数为 null 则返回 null
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * 格式化日期时间（使用指定格式）
     *
     * @param dateTime 日期时间
     * @param pattern  格式模式
     * @return 格式化字符串，如果参数为 null 则返回 null
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日期（使用默认格式 yyyy-MM-dd）
     *
     * @param date 日期
     * @return 格式化字符串，如果参数为 null 则返回 null
     */
    public static String format(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    /**
     * 格式化日期（使用指定格式）
     *
     * @param date    日期
     * @param pattern 格式模式
     * @return 格式化字符串，如果参数为 null 则返回 null
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    // ========== 解析方法 ==========

    /**
     * 解析日期时间字符串（使用默认格式 yyyy-MM-dd HH:mm:ss）
     *
     * @param text 日期时间字符串
     * @return 日期时间对象，如果参数为 null 或空则返回 null
     */
    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text, DATETIME_FORMATTER);
    }

    /**
     * 解析日期时间字符串（使用指定格式）
     *
     * @param text    日期时间字符串
     * @param pattern 格式模式
     * @return 日期时间对象，如果参数为 null 或空则返回 null
     */
    public static LocalDateTime parseDateTime(String text, String pattern) {
        if (text == null || text.trim().isEmpty() || pattern == null) {
            return null;
        }
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期字符串（使用默认格式 yyyy-MM-dd）
     *
     * @param text 日期字符串
     * @return 日期对象，如果参数为 null 或空则返回 null
     */
    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(text, DATE_FORMATTER);
    }

    /**
     * 解析日期字符串（使用指定格式）
     *
     * @param text    日期字符串
     * @param pattern 格式模式
     * @return 日期对象，如果参数为 null 或空则返回 null
     */
    public static LocalDate parseDate(String text, String pattern) {
        if (text == null || text.trim().isEmpty() || pattern == null) {
            return null;
        }
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    // ========== 类型转换 ==========

    /**
     * Date 转 LocalDateTime
     *
     * @param date Date 对象
     * @return LocalDateTime 对象，如果参数为 null 则返回 null
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Date 转 LocalDate
     *
     * @param date Date 对象
     * @return LocalDate 对象，如果参数为 null 则返回 null
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param dateTime LocalDateTime 对象
     * @return Date 对象，如果参数为 null 则返回 null
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDate 转 Date
     *
     * @param date LocalDate 对象
     * @return Date 对象，如果参数为 null 则返回 null
     */
    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 时间戳（毫秒）转 LocalDateTime
     *
     * @param timestamp 时间戳（毫秒）
     * @return LocalDateTime 对象
     */
    public static LocalDateTime ofTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime 转时间戳（毫秒）
     *
     * @param dateTime LocalDateTime 对象
     * @return 时间戳（毫秒），如果参数为 null 则返回 0
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ========== 日期计算 ==========

    /**
     * 计算两个日期之间的天数差
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 天数差（end - start）
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的小时差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时差（end - start）
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的分钟差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟差（end - start）
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    // ========== 边界获取 ==========

    /**
     * 获取一天的开始时间（00:00:00）
     *
     * @param dateTime 日期时间
     * @return 当天开始时间，如果参数为 null 则返回 null
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().atStartOfDay() : null;
    }

    /**
     * 获取一天的结束时间（23:59:59.999999999）
     *
     * @param dateTime 日期时间
     * @return 当天结束时间，如果参数为 null 则返回 null
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().atTime(LocalTime.MAX) : null;
    }

    /**
     * 获取本周第一天（周一）
     *
     * @param date 日期
     * @return 本周周一，如果参数为 null 则返回 null
     */
    public static LocalDate startOfWeek(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : null;
    }

    /**
     * 获取本周最后一天（周日）
     *
     * @param date 日期
     * @return 本周周日，如果参数为 null 则返回 null
     */
    public static LocalDate endOfWeek(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)) : null;
    }

    /**
     * 获取本月第一天
     *
     * @param date 日期
     * @return 本月第一天，如果参数为 null 则返回 null
     */
    public static LocalDate startOfMonth(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.firstDayOfMonth()) : null;
    }

    /**
     * 获取本月最后一天
     *
     * @param date 日期
     * @return 本月最后一天，如果参数为 null 则返回 null
     */
    public static LocalDate endOfMonth(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.lastDayOfMonth()) : null;
    }

    /**
     * 获取本年第一天
     *
     * @param date 日期
     * @return 本年第一天，如果参数为 null 则返回 null
     */
    public static LocalDate startOfYear(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.firstDayOfYear()) : null;
    }

    /**
     * 获取本年最后一天
     *
     * @param date 日期
     * @return 本年最后一天，如果参数为 null 则返回 null
     */
    public static LocalDate endOfYear(LocalDate date) {
        return date != null ? date.with(TemporalAdjusters.lastDayOfYear()) : null;
    }

    // ========== 便捷方法 ==========

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期
     *
     * @return 当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取当前日期时间字符串（yyyy-MM-dd HH:mm:ss）
     *
     * @return 当前日期时间字符串
     */
    public static String nowString() {
        return format(LocalDateTime.now());
    }

    /**
     * 获取当前日期字符串（yyyy-MM-dd）
     *
     * @return 当前日期字符串
     */
    public static String todayString() {
        return format(LocalDate.now());
    }

    /**
     * 判断是否为今天
     *
     * @param date 日期
     * @return 是否为今天
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }

    /**
     * 判断日期是否在指定范围内（包含边界）
     *
     * @param date  待检查日期
     * @param start 开始日期
     * @param end   结束日期
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 判断日期时间是否在指定范围内（包含边界）
     *
     * @param dateTime 待检查日期时间
     * @param start    开始时间
     * @param end      结束时间
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null || start == null || end == null) {
            return false;
        }
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }
}
