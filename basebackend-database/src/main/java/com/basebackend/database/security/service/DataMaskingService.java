package com.basebackend.database.security.service;

import com.basebackend.database.security.annotation.SensitiveType;

/**
 * 数据脱敏服务接口
 * 提供各种类型的数据脱敏功能
 */
public interface DataMaskingService {
    
    /**
     * 脱敏手机号
     * 格式：保留前3位和后4位，中间用*代替
     * 例如：138****5678
     * 
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    String maskPhone(String phone);
    
    /**
     * 脱敏身份证号
     * 格式：保留前6位和后4位，中间用*代替
     * 例如：110101********1234
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    String maskIdCard(String idCard);
    
    /**
     * 脱敏银行卡号
     * 格式：保留前4位和后4位，中间用*代替
     * 例如：6222 **** **** 1234
     * 
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    String maskBankCard(String bankCard);
    
    /**
     * 脱敏邮箱
     * 格式：保留邮箱前缀第一个字符和@后的域名，中间用*代替
     * 例如：u***@example.com
     * 
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    String maskEmail(String email);
    
    /**
     * 脱敏地址
     * 格式：只保留省份信息，其他用*代替
     * 例如：北京市***
     * 
     * @param address 地址
     * @return 脱敏后的地址
     */
    String maskAddress(String address);
    
    /**
     * 根据敏感类型自动脱敏
     * 
     * @param data 原始数据
     * @param type 敏感类型
     * @return 脱敏后的数据
     */
    String mask(String data, SensitiveType type);
    
    /**
     * 自定义脱敏规则
     * 
     * @param data 原始数据
     * @param rule 脱敏规则（例如："***-****-####" 表示保留后4位）
     * @return 脱敏后的数据
     */
    String maskWithRule(String data, String rule);
}
