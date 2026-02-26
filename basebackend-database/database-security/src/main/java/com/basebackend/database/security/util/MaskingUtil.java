package com.basebackend.database.security.util;

import com.basebackend.database.security.annotation.SensitiveType;
import com.basebackend.database.security.service.DataMaskingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 数据脱敏工具类
 * 提供静态方法便于在任何地方使用脱敏功能
 */
@Slf4j
@Component
public class MaskingUtil implements ApplicationContextAware {
    
    private static DataMaskingService dataMaskingService;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            dataMaskingService = applicationContext.getBean(DataMaskingService.class);
        } catch (Exception e) {
            log.warn("DataMaskingService not found, masking features will be disabled");
        }
    }
    
    /**
     * 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (dataMaskingService == null) {
            return phone;
        }
        return dataMaskingService.maskPhone(phone);
    }
    
    /**
     * 脱敏身份证号
     */
    public static String maskIdCard(String idCard) {
        if (dataMaskingService == null) {
            return idCard;
        }
        return dataMaskingService.maskIdCard(idCard);
    }
    
    /**
     * 脱敏银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (dataMaskingService == null) {
            return bankCard;
        }
        return dataMaskingService.maskBankCard(bankCard);
    }
    
    /**
     * 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (dataMaskingService == null) {
            return email;
        }
        return dataMaskingService.maskEmail(email);
    }
    
    /**
     * 脱敏地址
     */
    public static String maskAddress(String address) {
        if (dataMaskingService == null) {
            return address;
        }
        return dataMaskingService.maskAddress(address);
    }
    
    /**
     * 根据敏感类型自动脱敏
     */
    public static String mask(String data, SensitiveType type) {
        if (dataMaskingService == null) {
            return data;
        }
        return dataMaskingService.mask(data, type);
    }
    
    /**
     * 使用自定义规则脱敏
     */
    public static String maskWithRule(String data, String rule) {
        if (dataMaskingService == null) {
            return data;
        }
        return dataMaskingService.maskWithRule(data, rule);
    }
}
