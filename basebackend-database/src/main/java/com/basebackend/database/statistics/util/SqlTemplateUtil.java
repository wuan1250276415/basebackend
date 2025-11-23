package com.basebackend.database.statistics.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * SQL模板工具类
 * 用于生成SQL模板和计算MD5
 */
public class SqlTemplateUtil {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("'[^']*'");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 规范化SQL（移除多余空白）
     */
    public static String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return WHITESPACE_PATTERN.matcher(sql.trim()).replaceAll(" ");
    }

    /**
     * 生成SQL模板（参数化SQL）
     * 将具体的数值和字符串替换为占位符
     */
    public static String generateTemplate(String sql) {
        if (sql == null) {
            return "";
        }

        String template = sql;

        // Replace string literals with ?
        template = STRING_PATTERN.matcher(template).replaceAll("?");

        // Replace numbers with ?
        template = NUMBER_PATTERN.matcher(template).replaceAll("?");

        return template;
    }

    /**
     * 计算SQL模板的MD5值
     */
    public static String calculateMd5(String sqlTemplate) {
        if (sqlTemplate == null) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sqlTemplate.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
