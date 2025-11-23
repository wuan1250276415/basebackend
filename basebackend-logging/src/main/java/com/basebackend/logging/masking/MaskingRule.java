package com.basebackend.logging.masking;

/**
 * 脱敏规则配置
 *
 * 定义单个脱敏规则的详细信息，包括匹配方式和脱敏策略。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class MaskingRule {

    /**
     * 规则名称
     */
    private String name;

    /**
     * 正则表达式模式（用于字符串匹配）
     * 例如：手机号匹配 "\\b1\\d{10}\\b"
     */
    private String regex;

    /**
     * JSON路径（用于对象字段匹配）
     * 使用点分隔的路径，例如：user.phone、order.creditCard
     */
    private String jsonPath;

    /**
     * 脱敏策略
     * 默认为MASK策略
     */
    private MaskingStrategy strategy = MaskingStrategy.MASK;

    /**
     * 替换字符
     * 默认为"*"
     */
    private String replacement = "*";

    /**
     * 保留前缀字符数
     * 默认为3
     */
    private int prefixKeep = 3;

    /**
     * 保留后缀字符数
     * 默认为2
     */
    private int suffixKeep = 2;

    /**
     * 是否启用此规则
     * 默认为true
     */
    private boolean enabled = true;

    // ==================== Getter/Setter ====================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public MaskingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(MaskingStrategy strategy) {
        this.strategy = strategy;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public int getPrefixKeep() {
        return prefixKeep;
    }

    public void setPrefixKeep(int prefixKeep) {
        this.prefixKeep = prefixKeep;
    }

    public int getSuffixKeep() {
        return suffixKeep;
    }

    public void setSuffixKeep(int suffixKeep) {
        this.suffixKeep = suffixKeep;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "MaskingRule{" +
                "name='" + name + '\'' +
                ", regex='" + regex + '\'' +
                ", jsonPath='" + jsonPath + '\'' +
                ", strategy=" + strategy +
                ", enabled=" + enabled +
                '}';
    }
}
