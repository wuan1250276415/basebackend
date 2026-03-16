package com.basebackend.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.util.regex.Pattern;

/**
 * 基于 OWASP Java HTML Sanitizer 的输入输出清洗工具类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SanitizationUtils {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowCommonInlineFormattingElements()
            .allowCommonBlockElements()
            .toFactory()
            .and(Sanitizers.LINKS);

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(?is)("
                    + "<\\s*/?\\s*(?:script|iframe|object|embed|svg|math)\\b"
                    + "|<[^>]*\\bon[a-z][a-z0-9_:-]*\\s*="
                    + "|(?:javascript|vbscript|data)\\s*(?::|&#0*58;|&#x0*3a;)"
                    + ")");

    /**
     * 清洗字符串，移除潜在的 XSS 载荷
     *
     * @param raw 原始字符串
     * @return 清洗后的字符串
     */
    public static String sanitize(String raw) {
        if (StringUtils.isBlank(raw)) {
            return raw;
        }
        String trimmed = StringUtils.trim(raw);
        return POLICY.sanitize(trimmed);
    }

    /**
     * 判断字符串是否包含危险内容
     *
     * @param raw 原始字符串
     * @return true 表示包含危险片段
     */
    public static boolean containsUnsafeContent(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return false;
        }
        return DANGEROUS_PATTERN.matcher(raw).find();
    }
}
