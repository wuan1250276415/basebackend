package com.basebackend.common.util;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * <p>
 * 提供常用的字符串操作方法，补充 JDK 和常用库的不足。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 空值检查
 * boolean empty = StringUtils.isEmpty(str);
 * boolean blank = StringUtils.isBlank(str);
 *
 * // 默认值
 * String result = StringUtils.defaultIfBlank(str, "default");
 *
 * // 命名转换
 * String camel = StringUtils.toCamelCase("user_name"); // "userName"
 * String snake = StringUtils.toSnakeCase("userName"); // "user_name"
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== 空值检查 ==========

    /**
     * 判断字符串是否为空（null 或 ""）
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为空白（null 或全为空白字符）
     *
     * @param str 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否不为空白
     *
     * @param str 字符串
     * @return 是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 判断所有字符串是否都不为空白
     *
     * @param strs 字符串数组
     * @return 是否都不为空白
     */
    public static boolean isAllNotBlank(String... strs) {
        if (strs == null || strs.length == 0) {
            return false;
        }
        for (String str : strs) {
            if (isBlank(str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否存在至少一个空白字符串
     *
     * @param strs 字符串数组
     * @return 是否存在空白字符串
     */
    public static boolean isAnyBlank(String... strs) {
        if (strs == null || strs.length == 0) {
            return true;
        }
        for (String str : strs) {
            if (isBlank(str)) {
                return true;
            }
        }
        return false;
    }

    // ========== 默认值 ==========

    /**
     * 如果为空则返回默认值
     *
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * 如果为空白则返回默认值
     *
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * 如果为 null 则返回空字符串
     *
     * @param str 字符串
     * @return 字符串或空字符串
     */
    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    /**
     * 如果为空字符串则返回 null
     *
     * @param str 字符串
     * @return 字符串或 null
     */
    public static String emptyToNull(String str) {
        return isEmpty(str) ? null : str;
    }

    // ========== 去空白 ==========

    /**
     * 去除首尾空白（null 安全）
     *
     * @param str 字符串
     * @return 去除首尾空白后的字符串，null 返回 null
     */
    public static String trim(String str) {
        return str != null ? str.trim() : null;
    }

    /**
     * 去除首尾空白，null 返回空字符串
     *
     * @param str 字符串
     * @return 去除首尾空白后的字符串
     */
    public static String trimToEmpty(String str) {
        return str != null ? str.trim() : "";
    }

    /**
     * 去除首尾空白，空白结果返回 null
     *
     * @param str 字符串
     * @return 去除首尾空白后的字符串，空白返回 null
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }

    // ========== 命名转换 ==========

    /** 下划线匹配模式 */
    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_([a-z])");

    /** 大写字母匹配模式 */
    private static final Pattern CAMEL_PATTERN = Pattern.compile("([a-z])([A-Z])");

    /**
     * 下划线命名转驼峰命名（首字母小写）
     * <p>
     * 例如：user_name -> userName
     * </p>
     *
     * @param str 下划线命名字符串
     * @return 驼峰命名字符串
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        str = str.toLowerCase();
        StringBuilder sb = new StringBuilder(str.length());
        boolean upperCase = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 下划线命名转帕斯卡命名（首字母大写）
     * <p>
     * 例如：user_name -> UserName
     * </p>
     *
     * @param str 下划线命名字符串
     * @return 帕斯卡命名字符串
     */
    public static String toPascalCase(String str) {
        String camel = toCamelCase(str);
        if (isEmpty(camel)) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * 驼峰命名转下划线命名
     * <p>
     * 例如：userName -> user_name
     * </p>
     *
     * @param str 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 4);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 驼峰命名转中划线命名（kebab-case）
     * <p>
     * 例如：userName -> user-name
     * </p>
     *
     * @param str 驼峰命名字符串
     * @return 中划线命名字符串
     */
    public static String toKebabCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 4);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ========== 截取和填充 ==========

    /**
     * 截取字符串（null 安全）
     *
     * @param str   字符串
     * @param start 开始位置
     * @param end   结束位置
     * @return 截取后的字符串
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (start < 0) {
            start = 0;
        }
        if (end > len) {
            end = len;
        }
        if (start > end) {
            return "";
        }
        return str.substring(start, end);
    }

    /**
     * 截取字符串，超出部分用省略号替代
     *
     * @param str       字符串
     * @param maxLength 最大长度
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        if (maxLength <= 3) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 左填充字符串
     *
     * @param str    字符串
     * @param length 目标长度
     * @param padStr 填充字符
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int length, char padStr) {
        if (str == null) {
            str = "";
        }
        int padLen = length - str.length();
        if (padLen <= 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < padLen; i++) {
            sb.append(padStr);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 右填充字符串
     *
     * @param str    字符串
     * @param length 目标长度
     * @param padStr 填充字符
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int length, char padStr) {
        if (str == null) {
            str = "";
        }
        int padLen = length - str.length();
        if (padLen <= 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(str);
        for (int i = 0; i < padLen; i++) {
            sb.append(padStr);
        }
        return sb.toString();
    }

    // ========== 连接 ==========

    /**
     * 连接字符串数组
     *
     * @param delimiter 分隔符
     * @param elements  字符串数组
     * @return 连接后的字符串
     */
    public static String join(String delimiter, String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(delimiter != null ? delimiter : "");
        for (String element : elements) {
            if (element != null) {
                joiner.add(element);
            }
        }
        return joiner.toString();
    }

    /**
     * 连接集合
     *
     * @param delimiter  分隔符
     * @param collection 集合
     * @return 连接后的字符串
     */
    public static String join(String delimiter, Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(delimiter != null ? delimiter : "");
        for (Object element : collection) {
            if (element != null) {
                joiner.add(element.toString());
            }
        }
        return joiner.toString();
    }

    // ========== 格式化 ==========

    /**
     * 格式化字符串（类似 SLF4J 的 {} 占位符）
     * <p>
     * 例如：format("Hello, {}!", "World") -> "Hello, World!"
     * </p>
     *
     * @param template 模板字符串
     * @param args     参数
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 50);
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            if (i < template.length() - 1 && template.charAt(i) == '{' && template.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(template.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // ========== 包含检查 ==========

    /**
     * 判断字符串是否包含指定子串（null 安全）
     *
     * @param str       字符串
     * @param searchStr 搜索子串
     * @return 是否包含
     */
    public static boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.contains(searchStr);
    }

    /**
     * 判断字符串是否包含指定子串（忽略大小写）
     *
     * @param str       字符串
     * @param searchStr 搜索子串
     * @return 是否包含
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.toLowerCase().contains(searchStr.toLowerCase());
    }

    /**
     * 判断字符串是否以指定前缀开头（null 安全）
     *
     * @param str    字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头
     */
    public static boolean startsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.startsWith(prefix);
    }

    /**
     * 判断字符串是否以指定后缀结尾（null 安全）
     *
     * @param str    字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾
     */
    public static boolean endsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        return str.endsWith(suffix);
    }
}
