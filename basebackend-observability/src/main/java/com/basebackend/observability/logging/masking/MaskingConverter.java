package com.basebackend.observability.logging.masking;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.basebackend.observability.logging.config.LoggingProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback 日志脱敏转换器
 * <p>
 * 基于正则表达式对日志消息中的敏感信息进行脱敏处理。
 * 支持通过 LoggingProperties 配置动态脱敏规则。
 * </p>
 * <p>
 * <b>支持的脱敏策略：</b>
 * <ul>
 *     <li>PARTIAL: 部分显示（如手机号：138****1234）</li>
 *     <li>HIDE: 完全隐藏（替换为 ******）</li>
 *     <li>HASH: 哈希化（SHA-256）</li>
 * </ul>
 * </p>
 * <p>
 * <b>预置脱敏规则：</b>
 * <ul>
 *     <li>手机号：13800138000 → 138****8000</li>
 *     <li>身份证：110101199001011234 → 110101********1234</li>
 *     <li>邮箱：user@example.com → u***@example.com</li>
 *     <li>密码：password=xxx → password=******</li>
 *     <li>银行卡：6222021234567890 → 6222********7890</li>
 * </ul>
 * </p>
 * <p>
 * <b>配置示例（logback-spring.xml）：</b>
 * <pre>{@code
 * <conversionRule conversionWord="msg"
 *                 converterClass="com.basebackend.observability.logging.masking.MaskingConverter" />
 * }</pre>
 * </p>
 * <p>
 * <b>动态规则配置：</b>
 * 通过 LoggingAutoConfiguration 调用 setConfiguredRules() 方法注入配置规则。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class MaskingConverter extends MessageConverter {

    /**
     * 预置脱敏规则列表（默认规则）
     */
    private static final List<MaskingRule> DEFAULT_RULES = new ArrayList<>();

    /**
     * 配置化脱敏规则（从 LoggingProperties 加载）
     */
    private static volatile List<MaskingRule> configuredRules = Collections.emptyList();

    static {
        // 手机号脱敏：保留前3位和后4位
        DEFAULT_RULES.add(new MaskingRule(
                "mobile",
                Pattern.compile("(\\d{3})\\d{4}(\\d{4})"),
                MaskingStrategy.PARTIAL,
                "$1****$2"
        ));

        // 身份证脱敏：保留前6位和后4位
        DEFAULT_RULES.add(new MaskingRule(
                "idCard",
                Pattern.compile("(\\d{6})\\d{8}(\\d{4})"),
                MaskingStrategy.PARTIAL,
                "$1********$2"
        ));

        // 邮箱脱敏：保留首字符和域名
        DEFAULT_RULES.add(new MaskingRule(
                "email",
                Pattern.compile("(\\w)[\\w.]*@([\\w.-]+)"),
                MaskingStrategy.PARTIAL,
                "$1***@$2"
        ));

        // 密码脱敏：完全隐藏（修复：支持空格和引号）
        DEFAULT_RULES.add(new MaskingRule(
                "password",
                Pattern.compile("(password|pwd|passwd)\\s*[:=]\\s*\"?([^\"\\s]+(?:\\s+[^\"\\s]+)*)\"?",
                        Pattern.CASE_INSENSITIVE),
                MaskingStrategy.HIDE,
                "$1=******"
        ));

        // 银行卡号脱敏：保留前4位和后4位
        DEFAULT_RULES.add(new MaskingRule(
                "bankCard",
                Pattern.compile("(\\d{4})\\d{8,12}(\\d{4})"),
                MaskingStrategy.PARTIAL,
                "$1********$2"
        ));
    }

    /**
     * 设置配置化脱敏规则
     * <p>
     * 由 LoggingAutoConfiguration 调用，注入从 LoggingProperties 加载的规则。
     * </p>
     *
     * @param rules 脱敏规则列表
     */
    public static void setConfiguredRules(List<LoggingProperties.MaskingRule> rules) {
        if (rules == null || rules.isEmpty()) {
            configuredRules = Collections.emptyList();
            return;
        }

        List<MaskingRule> converted = new ArrayList<>();
        for (LoggingProperties.MaskingRule rule : rules) {
            try {
                Pattern pattern = Pattern.compile(rule.getFieldPattern(), Pattern.CASE_INSENSITIVE);
                MaskingStrategy strategy = MaskingStrategy.valueOf(rule.getStrategy().toUpperCase());
                converted.add(new MaskingRule(
                        rule.getFieldPattern(),
                        pattern,
                        strategy,
                        rule.getPartialPattern()
                ));
            } catch (Exception e) {
                // 规则编译失败时静默忽略
            }
        }
        configuredRules = Collections.unmodifiableList(converted);
    }

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = event.getFormattedMessage();
        if (originalMessage == null || originalMessage.isEmpty()) {
            return originalMessage;
        }

        return maskSensitiveData(originalMessage);
    }

    /**
     * 对敏感数据进行脱敏
     *
     * @param message 原始消息
     * @return 脱敏后的消息
     */
    private String maskSensitiveData(String message) {
        String maskedMessage = message;

        // 先应用配置规则
        for (MaskingRule rule : configuredRules) {
            maskedMessage = applyRule(maskedMessage, rule);
        }

        // 再应用默认规则
        for (MaskingRule rule : DEFAULT_RULES) {
            maskedMessage = applyRule(maskedMessage, rule);
        }

        return maskedMessage;
    }

    /**
     * 应用单个脱敏规则
     *
     * @param message 消息
     * @param rule    脱敏规则
     * @return 脱敏后的消息
     */
    private String applyRule(String message, MaskingRule rule) {
        try {
            Matcher matcher = rule.pattern.matcher(message);

            switch (rule.strategy) {
                case HIDE:
                    return matcher.replaceAll(rule.replacement);

                case HASH:
                    return matcher.replaceAll(matchResult -> sha256Hash(matchResult.group()));

                case PARTIAL:
                default:
                    return matcher.replaceAll(rule.replacement);
            }
        } catch (Exception e) {
            // 脱敏失败时静默忽略，不影响日志输出
            return message;
        }
    }

    /**
     * SHA-256 哈希（用于 HASH 策略）
     *
     * @param input 输入字符串
     * @return Base64 编码的哈希值
     */
    private static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }

    /**
     * 脱敏规则定义
     */
    private static class MaskingRule {
        final String name;
        final Pattern pattern;
        final MaskingStrategy strategy;
        final String replacement;

        MaskingRule(String name, Pattern pattern, MaskingStrategy strategy, String replacement) {
            this.name = name;
            this.pattern = pattern;
            this.strategy = strategy;
            this.replacement = replacement;
        }
    }

    /**
     * 脱敏策略枚举
     */
    private enum MaskingStrategy {
        PARTIAL,  // 部分显示
        HIDE,     // 完全隐藏
        HASH      // 哈希化
    }
}
