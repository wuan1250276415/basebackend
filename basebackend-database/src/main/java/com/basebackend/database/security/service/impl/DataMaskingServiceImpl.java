package com.basebackend.database.security.service.impl;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.annotation.SensitiveType;
import com.basebackend.database.security.service.DataMaskingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务实现
 * 提供各种常见类型的数据脱敏功能
 */
@Slf4j
@Service
public class DataMaskingServiceImpl implements DataMaskingService {
    
    private static final String MASK_CHAR = "*";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    
    private final boolean enabled;
    private final Map<String, String> customRules;
    
    public DataMaskingServiceImpl(DatabaseEnhancedProperties properties) {
        this.enabled = properties.getSecurity().getMasking().isEnabled();
        this.customRules = properties.getSecurity().getMasking().getRules();
    }
    
    @Override
    public String maskPhone(String phone) {
        if (!enabled || !StringUtils.hasText(phone)) {
            return phone;
        }
        
        // 验证手机号格式
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            log.warn("Invalid phone format: {}", phone);
            return phone;
        }
        
        // 保留前3位和后4位，中间用*代替
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    @Override
    public String maskIdCard(String idCard) {
        if (!enabled || !StringUtils.hasText(idCard)) {
            return idCard;
        }
        
        // 验证身份证号格式
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            log.warn("Invalid ID card format: {}", idCard);
            return idCard;
        }
        
        // 保留前6位和后4位，中间用*代替
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
    
    @Override
    public String maskBankCard(String bankCard) {
        if (!enabled || !StringUtils.hasText(bankCard)) {
            return bankCard;
        }
        
        // 移除空格
        String cleanCard = bankCard.replaceAll("\\s+", "");
        
        // 验证银行卡号格式
        if (!BANK_CARD_PATTERN.matcher(cleanCard).matches()) {
            log.warn("Invalid bank card format: {}", bankCard);
            return bankCard;
        }
        
        // 保留前4位和后4位，中间用*代替，每4位加一个空格
        String prefix = cleanCard.substring(0, 4);
        String suffix = cleanCard.substring(cleanCard.length() - 4);
        int maskLength = cleanCard.length() - 8;
        
        StringBuilder masked = new StringBuilder(prefix);
        masked.append(" ");
        
        // 添加中间的*号，每4位加一个空格
        for (int i = 0; i < maskLength; i++) {
            if (i > 0 && i % 4 == 0) {
                masked.append(" ");
            }
            masked.append(MASK_CHAR);
        }
        
        masked.append(" ").append(suffix);
        return masked.toString();
    }
    
    @Override
    public String maskEmail(String email) {
        if (!enabled || !StringUtils.hasText(email)) {
            return email;
        }
        
        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.warn("Invalid email format: {}", email);
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        // 保留前缀第一个字符，其他用*代替
        if (prefix.length() == 1) {
            return prefix + MASK_CHAR + domain;
        } else if (prefix.length() == 2) {
            return prefix.charAt(0) + MASK_CHAR + domain;
        } else {
            return prefix.charAt(0) + MASK_CHAR.repeat(prefix.length() - 1) + domain;
        }
    }
    
    @Override
    public String maskAddress(String address) {
        if (!enabled || !StringUtils.hasText(address)) {
            return address;
        }
        
        // 简单实现：保留前3个字符（通常是省份），其他用*代替
        if (address.length() <= 3) {
            return address;
        }
        
        return address.substring(0, 3) + MASK_CHAR.repeat(Math.min(address.length() - 3, 10));
    }
    
    @Override
    public String mask(String data, SensitiveType type) {
        if (!enabled || !StringUtils.hasText(data)) {
            return data;
        }
        
        return switch (type) {
            case PHONE -> maskPhone(data);
            case ID_CARD -> maskIdCard(data);
            case BANK_CARD -> maskBankCard(data);
            case EMAIL -> maskEmail(data);
            case ADDRESS -> maskAddress(data);
            case PASSWORD -> MASK_CHAR.repeat(6); // 密码完全隐藏
            case CUSTOM -> {
                // 尝试从配置中获取自定义规则
                String rule = customRules.get("custom");
                if (StringUtils.hasText(rule)) {
                    yield maskWithRule(data, rule);
                }
                // 默认脱敏：保留前后各1个字符
                if (data.length() <= 2) {
                    yield MASK_CHAR.repeat(data.length());
                }
                yield data.charAt(0) + MASK_CHAR.repeat(data.length() - 2) + data.charAt(data.length() - 1);
            }
        };
    }
    
    @Override
    public String maskWithRule(String data, String rule) {
        if (!enabled || !StringUtils.hasText(data) || !StringUtils.hasText(rule)) {
            return data;
        }
        
        try {
            return applyMaskingRule(data, rule);
        } catch (Exception e) {
            log.error("Failed to apply masking rule: {}", rule, e);
            return data;
        }
    }
    
    /**
     * 应用脱敏规则
     * 规则格式说明：
     * - # 表示保留原字符
     * - * 表示用*替换
     * - 其他字符表示直接使用该字符
     * 
     * 例如：
     * - "###****####" 表示保留前3位和后4位，中间用*代替
     * - "***-****-####" 表示保留后4位，其他用*代替，并添加分隔符
     */
    private String applyMaskingRule(String data, String rule) {
        StringBuilder result = new StringBuilder();
        int dataIndex = 0;
        int dataLength = data.length();
        
        // 计算规则中#的数量（需要保留的字符数）
        long keepCount = rule.chars().filter(ch -> ch == '#').count();
        
        // 如果数据长度小于需要保留的字符数，直接返回原数据
        if (dataLength < keepCount) {
            return data;
        }
        
        // 从后往前计算需要保留的后缀位置
        int suffixStart = (int) (dataLength - rule.chars().filter(ch -> ch == '#').skip(rule.indexOf('#')).count());
        
        for (int i = 0; i < rule.length() && dataIndex < dataLength; i++) {
            char ruleChar = rule.charAt(i);
            
            if (ruleChar == '#') {
                // 保留原字符
                result.append(data.charAt(dataIndex));
                dataIndex++;
            } else if (ruleChar == '*') {
                // 用*替换
                if (dataIndex < dataLength) {
                    result.append(MASK_CHAR);
                    dataIndex++;
                }
            } else {
                // 直接使用规则中的字符（如分隔符）
                result.append(ruleChar);
            }
        }
        
        return result.toString();
    }
}
