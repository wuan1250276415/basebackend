package com.basebackend.scheduler.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Date 和 Instant 类型转换工具类
 * 
 * <p>用于处理 Camunda API (使用 java.util.Date) 和现代 Java 时间 API (使用 java.time.Instant) 之间的转换
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class DateTimeConverter {

    private DateTimeConverter() {
        // Utility class
    }

    /**
     * 将 Instant 转换为 Date
     *
     * @param instant Instant 对象
     * @return Date 对象，如果 instant 为 null 则返回 null
     */
    public static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    /**
     * 将 Date 转换为 Instant
     *
     * @param date Date 对象
     * @return Instant 对象，如果 date 为 null 则返回 null
     */
    public static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    /**
     * 将 LocalDateTime 转换为 Date
     *
     * @param localDateTime LocalDateTime 对象
     * @return Date 对象，如果 localDateTime 为 null 则返回 null
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将 Date 转换为 LocalDateTime
     *
     * @param date Date 对象
     * @return LocalDateTime 对象，如果 date 为 null 则返回 null
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
