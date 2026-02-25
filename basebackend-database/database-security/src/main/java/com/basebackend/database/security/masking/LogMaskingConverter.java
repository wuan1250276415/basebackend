package com.basebackend.database.security.masking;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志脱敏转换器
 * 用于Logback日志框架，自动脱敏日志中的敏感信息
 * 
 * 配置方式（在logback-spring.xml中）：
 * <pre>
 * {@code
 * <conversionRule conversionWord="msg" 
 *                 converterClass="com.basebackend.database.security.masking.LogMaskingConverter" />
 * <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
 * }
 * </pre>
 */
@Slf4j
public class LogMaskingConverter extends MessageConverter {
    
    // 手机号正则：1开头的11位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    
    // 身份证号正则：18位数字或17位数字+X
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");
    
    // 银行卡号正则：16-19位数字
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");
    
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");
    
    // 密码相关关键字（后面跟着的内容需要脱敏）
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|pwd|passwd)\\s*[=:]\\s*([^\\s,;]+)", Pattern.CASE_INSENSITIVE);
    
    private static final String MASK_CHAR = "*";
    
    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        try {
            // 脱敏手机号
            message = maskPattern(message, PHONE_PATTERN, this::maskPhone);
            
            // 脱敏身份证号
            message = maskPattern(message, ID_CARD_PATTERN, this::maskIdCard);
            
            // 脱敏银行卡号
            message = maskPattern(message, BANK_CARD_PATTERN, this::maskBankCard);
            
            // 脱敏邮箱
            message = maskPattern(message, EMAIL_PATTERN, this::maskEmail);
            
            // 脱敏密码
            message = maskPassword(message);
            
        } catch (Exception e) {
            // 脱敏失败不应该影响日志输出
            log.error("Failed to mask sensitive data in log", e);
        }
        
        return message;
    }
    
    /**
     * 使用正则模式脱敏
     */
    private String maskPattern(String message, Pattern pattern, MaskFunction maskFunction) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String matched = matcher.group();
            String masked = maskFunction.mask(matched);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * 脱敏手机号
     */
    private String maskPhone(String phone) {
        if (phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 脱敏身份证号
     */
    private String maskIdCard(String idCard) {
        if (idCard.length() != 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
    
    /**
     * 脱敏银行卡号
     */
    private String maskBankCard(String bankCard) {
        if (bankCard.length() < 8) {
            return MASK_CHAR.repeat(bankCard.length());
        }
        String prefix = bankCard.substring(0, 4);
        String suffix = bankCard.substring(bankCard.length() - 4);
        int maskLength = bankCard.length() - 8;
        return prefix + MASK_CHAR.repeat(maskLength) + suffix;
    }
    
    /**
     * 脱敏邮箱
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String prefix = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (prefix.length() <= 1) {
            return prefix + MASK_CHAR + domain;
        }
        
        return prefix.charAt(0) + MASK_CHAR.repeat(prefix.length() - 1) + domain;
    }
    
    /**
     * 脱敏密码
     */
    private String maskPassword(String message) {
        Matcher matcher = PASSWORD_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = key + matcher.group(0).substring(key.length(), matcher.group(0).indexOf(matcher.group(2))) + MASK_CHAR.repeat(6);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * 脱敏函数接口
     */
    @FunctionalInterface
    private interface MaskFunction {
        String mask(String input);
    }
}
