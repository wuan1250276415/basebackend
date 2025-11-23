package com.basebackend.database.security.annotation;

/**
 * 敏感数据类型枚举
 */
public enum SensitiveType {
    /**
     * 自定义类型
     */
    CUSTOM,
    
    /**
     * 手机号
     */
    PHONE,
    
    /**
     * 身份证号
     */
    ID_CARD,
    
    /**
     * 银行卡号
     */
    BANK_CARD,
    
    /**
     * 邮箱
     */
    EMAIL,
    
    /**
     * 密码
     */
    PASSWORD,
    
    /**
     * 地址
     */
    ADDRESS
}
