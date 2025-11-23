package com.basebackend.logging.masking;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 脱敏系统配置属性
 *
 * 通过 Spring Boot ConfigurationProperties 自动绑定配置。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@ConfigurationProperties(prefix = "basebackend.logging.masking")
public class MaskingProperties {

    /**
     * 是否启用脱敏功能
     */
    private boolean enabled = true;

    /**
     * 脱敏规则列表
     */
    private List<MaskingRule> rules = defaultRules();

    /**
     * 忽略的日志器名称（不进行脱敏）
     */
    private List<String> ignoreLoggers = new ArrayList<>();

    /**
     * 慢脱敏阈值（毫秒）
     * 超过此时间的脱敏操作会记录警告
     */
    private long slowThresholdMillis = 5;

    // ==================== Getter/Setter ====================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<MaskingRule> getRules() {
        return rules;
    }

    public void setRules(List<MaskingRule> rules) {
        this.rules = rules;
    }

    public List<String> getIgnoreLoggers() {
        return ignoreLoggers;
    }

    public void setIgnoreLoggers(List<String> ignoreLoggers) {
        this.ignoreLoggers = ignoreLoggers;
    }

    public long getSlowThresholdMillis() {
        return slowThresholdMillis;
    }

    public void setSlowThresholdMillis(long slowThresholdMillis) {
        this.slowThresholdMillis = slowThresholdMillis;
    }

    /**
     * 创建默认脱敏规则
     */
    private List<MaskingRule> defaultRules() {
        List<MaskingRule> list = new ArrayList<>();

        // 手机号脱敏规则
        MaskingRule phone = new MaskingRule();
        phone.setName("phone");
        phone.setRegex("\\b1\\d{10}\\b");
        phone.setStrategy(MaskingStrategy.PARTIAL);
        phone.setPrefixKeep(3);
        phone.setSuffixKeep(2);
        list.add(phone);

        // 身份证脱敏规则
        MaskingRule idCard = new MaskingRule();
        idCard.setName("id-card");
        idCard.setRegex("\\b\\d{6}(19|20)\\d{2}\\d{2}\\d{2}\\d{3}[0-9Xx]\\b");
        idCard.setStrategy(MaskingStrategy.PARTIAL);
        idCard.setPrefixKeep(2);
        idCard.setSuffixKeep(2);
        list.add(idCard);

        // 银行卡脱敏规则
        MaskingRule bank = new MaskingRule();
        bank.setName("bank-card");
        bank.setRegex("\\b\\d{16,19}\\b");
        bank.setStrategy(MaskingStrategy.PARTIAL);
        bank.setPrefixKeep(4);
        bank.setSuffixKeep(4);
        list.add(bank);

        // 邮箱脱敏规则
        MaskingRule email = new MaskingRule();
        email.setName("email");
        email.setRegex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
        email.setStrategy(MaskingStrategy.MASK);
        email.setPrefixKeep(2);
        email.setSuffixKeep(6);
        list.add(email);

        return list;
    }
}
