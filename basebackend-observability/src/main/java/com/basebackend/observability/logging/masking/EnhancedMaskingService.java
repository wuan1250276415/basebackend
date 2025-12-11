package com.basebackend.observability.logging.masking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强版日志脱敏服务
 * <p>
 * 改进点：
 * - 支持注册自定义脱敏规则
 * - 预定义常见敏感信息模式
 * - 高效的正则匹配
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class EnhancedMaskingService {

    /** 脱敏规则：Pattern -> Strategy */
    private final Map<Pattern, MaskingStrategy> maskingRules = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册预定义的脱敏规则
        registerDefaultRules();
        log.info("EnhancedMaskingService initialized with {} rules", maskingRules.size());
    }

    /**
     * 注册默认的脱敏规则
     */
    private void registerDefaultRules() {
        // 手机号
        registerRule("1[3-9]\\d{9}", MaskingStrategy.PHONE);

        // 身份证号
        registerRule("\\d{17}[\\dXx]", MaskingStrategy.ID_CARD);
        registerRule("\\d{15}", MaskingStrategy.ID_CARD);

        // 邮箱
        registerRule("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}", MaskingStrategy.EMAIL);

        // 银行卡号（16-19位数字）
        registerRule("\\d{16,19}", MaskingStrategy.BANK_CARD);

        // 密码相关关键字后的值
        registerRule("(?i)(password|passwd|pwd|secret|token|apikey|api_key)\\s*[=:]\\s*[\"']?([^\"'\\s]+)[\"']?",
                input -> input.replaceAll(
                        "(?i)(password|passwd|pwd|secret|token|apikey|api_key)(\\s*[=:]\\s*[\"']?)([^\"'\\s]+)([\"']?)",
                        "$1$2******$4"));
    }

    /**
     * 注册自定义脱敏规则
     *
     * @param regex    正则表达式
     * @param strategy 脱敏策略
     */
    public void registerRule(String regex, MaskingStrategy strategy) {
        Pattern pattern = Pattern.compile(regex);
        maskingRules.put(pattern, strategy);
        log.debug("Registered masking rule: {}", regex);
    }

    /**
     * 对文本进行脱敏处理
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        for (Map.Entry<Pattern, MaskingStrategy> entry : maskingRules.entrySet()) {
            Pattern pattern = entry.getKey();
            MaskingStrategy strategy = entry.getValue();

            Matcher matcher = pattern.matcher(result);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String matched = matcher.group();
                String masked = strategy.mask(matched);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    /**
     * 检查文本是否包含敏感信息
     *
     * @param text 文本
     * @return 是否包含敏感信息
     */
    public boolean containsSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (Pattern pattern : maskingRules.keySet()) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取已注册的规则数量
     */
    public int getRuleCount() {
        return maskingRules.size();
    }

    /**
     * 清除所有规则
     */
    public void clearRules() {
        maskingRules.clear();
    }
}
