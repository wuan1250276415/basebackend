package com.basebackend.scheduler.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日期时间转换工具类
 * 用于处理 java.util.Date 和 java.time.Instant/LocalDateTime 之间的转换
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public class DateTimeUtil {

    private DateTimeUtil() {
        // 工具类，禁止实例化
    }

    /**
     * Instant 转 Date
     *
     * @param instant Instant 对象
     * @return Date 对象，如果 instant 为 null 则返回 null
     */
    public static Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }

    /**
     * Date 转 Instant
     *
     * @param date Date 对象
     * @return Instant 对象，如果 date 为 null 则返回 null
     */
    public static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    /**
     * Date 转 LocalDateTime
     *
     * @param date Date 对象
     * @return LocalDateTime 对象，如果 date 为 null 则返回 null
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return date != null ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()) : null;
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param localDateTime LocalDateTime 对象
     * @return Date 对象，如果 localDateTime 为 null 则返回 null
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return localDateTime != null ? Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    /**
     * Instant 转 LocalDateTime
     *
     * @param instant Instant 对象
     * @return LocalDateTime 对象，如果 instant 为 null 则返回 null
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    /**
     * LocalDateTime 转 Instant
     *
     * @param localDateTime LocalDateTime 对象
     * @return Instant 对象，如果 localDateTime 为 null 则返回 null
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
