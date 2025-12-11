package com.basebackend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL注入验证器实现
 * <p>
 * 检测常见的SQL注入攻击模式。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class NoSqlInjectionValidator implements ConstraintValidator<NoSqlInjection, String> {

    /**
     * 基础SQL关键字（不区分大小写）
     */
    private static final Set<String> BASIC_SQL_KEYWORDS = Set.of(
            "select", "insert", "update", "delete", "drop", "truncate",
            "exec", "execute", "xp_", "sp_", "union", "into",
            "grant", "revoke", "create", "alter", "index", "table");

    /**
     * 严格模式下的额外SQL关键字
     */
    private static final Set<String> STRICT_SQL_KEYWORDS = Set.of(
            "where", "having", "group", "order", "limit", "offset",
            "join", "inner", "outer", "left", "right", "cross",
            "like", "between", "exists", "in", "and", "or", "not",
            "null", "is", "case", "when", "then", "else", "end",
            "declare", "set", "cursor", "fetch", "open", "close",
            "begin", "commit", "rollback", "savepoint", "transaction");

    /**
     * 危险的SQL注入模式
     */
    private static final Pattern[] DANGEROUS_PATTERNS = {
            // 注释
            Pattern.compile("--.*$", Pattern.MULTILINE),
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL),
            Pattern.compile("#.*$", Pattern.MULTILINE),

            // 字符串拼接
            Pattern.compile("'\\s*;"),
            Pattern.compile("'\\s*\\+\\s*'"),
            Pattern.compile("\"\\s*\\+\\s*\""),

            // 逻辑操作
            Pattern.compile("'\\s*OR\\s+'\\s*=\\s*'", Pattern.CASE_INSENSITIVE),
            Pattern.compile("'\\s*OR\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+\\s*=\\s*\\d+\\s*--", Pattern.CASE_INSENSITIVE),
            Pattern.compile("1\\s*=\\s*1", Pattern.CASE_INSENSITIVE),
            Pattern.compile("'\\s*=\\s*'"),

            // 联合查询
            Pattern.compile("UNION\\s+(ALL\\s+)?SELECT", Pattern.CASE_INSENSITIVE),

            // 堆叠查询
            Pattern.compile(";\\s*(SELECT|INSERT|UPDATE|DELETE|DROP|EXEC)", Pattern.CASE_INSENSITIVE),

            // 函数调用
            Pattern.compile("(CHAR|CHR|ASCII|CONCAT|SUBSTRING|MID)\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(SLEEP|BENCHMARK|WAITFOR|DELAY)\\s*\\(", Pattern.CASE_INSENSITIVE),

            // 系统命令
            Pattern.compile("(xp_cmdshell|sp_executesql|OPENROWSET|OPENDATASOURCE)", Pattern.CASE_INSENSITIVE),

            // 十六进制和编码
            Pattern.compile("0x[0-9a-fA-F]+"),
            Pattern.compile("CHAR\\s*\\(\\s*\\d+\\s*\\)", Pattern.CASE_INSENSITIVE),

            // 空字节注入
            Pattern.compile("\\x00"),
            Pattern.compile("%00")
    };

    /**
     * 严格模式下的额外危险模式
     */
    private static final Pattern[] STRICT_PATTERNS = {
            // 单引号相关
            Pattern.compile("'\\s*$"),
            Pattern.compile("^\\s*'"),
            Pattern.compile("''"),

            // 括号不平衡
            Pattern.compile("\\(\\s*\\)"),

            // 特殊符号组合
            Pattern.compile("\\|\\|"),
            Pattern.compile("&&"),
            Pattern.compile(">>"),
            Pattern.compile("<<")
    };

    private boolean strict;

    @Override
    public void initialize(NoSqlInjection constraintAnnotation) {
        this.strict = constraintAnnotation.strict();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true; // 空值由其他注解处理（如@NotBlank）
        }

        // 检查危险模式
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return false;
            }
        }

        // 检查基础SQL关键字
        String lowerValue = value.toLowerCase();
        for (String keyword : BASIC_SQL_KEYWORDS) {
            // 检查是否作为独立词出现（前后有边界）
            if (containsKeywordAsBoundary(lowerValue, keyword)) {
                return false;
            }
        }

        // 严格模式下的额外检查
        if (strict) {
            for (Pattern pattern : STRICT_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    return false;
                }
            }

            for (String keyword : STRICT_SQL_KEYWORDS) {
                if (containsKeywordAsBoundary(lowerValue, keyword)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 检查关键字是否作为独立词出现
     */
    private boolean containsKeywordAsBoundary(String text, String keyword) {
        int index = text.indexOf(keyword);
        while (index >= 0) {
            boolean leftBoundary = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            boolean rightBoundary = index + keyword.length() >= text.length()
                    || !Character.isLetterOrDigit(text.charAt(index + keyword.length()));

            if (leftBoundary && rightBoundary) {
                return true;
            }

            index = text.indexOf(keyword, index + 1);
        }
        return false;
    }

    /**
     * 静态方法：检查字符串是否包含SQL注入
     *
     * @param value 待检查的字符串
     * @return true 如果包含SQL注入攻击载荷
     */
    public static boolean containsSqlInjection(String value) {
        return containsSqlInjection(value, false);
    }

    /**
     * 静态方法：检查字符串是否包含SQL注入
     *
     * @param value  待检查的字符串
     * @param strict 是否使用严格模式
     * @return true 如果包含SQL注入攻击载荷
     */
    public static boolean containsSqlInjection(String value, boolean strict) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        NoSqlInjectionValidator validator = new NoSqlInjectionValidator();
        validator.strict = strict;
        return !validator.isValid(value, null);
    }
}
