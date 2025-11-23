package com.basebackend.database.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段注解
 * 标记需要加密存储和权限控制的字段
 * 
 * 使用示例：
 * <pre>
 * public class User {
 *     // 需要VIEW_PHONE权限才能查看未脱敏的手机号
 *     @Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_PHONE")
 *     private String phone;
 *     
 *     // 需要VIEW_ID_CARD权限才能查看未脱敏的身份证号
 *     @Sensitive(type = SensitiveType.ID_CARD, requiredPermission = "VIEW_ID_CARD")
 *     private String idCard;
 *     
 *     // 不需要特定权限，但会根据VIEW_SENSITIVE_DATA权限决定是否脱敏
 *     @Sensitive
 *     private String address;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    
    /**
     * 敏感类型
     */
    SensitiveType type() default SensitiveType.CUSTOM;
    
    /**
     * 是否加密存储
     * 默认为true，表示需要加密
     */
    boolean encrypt() default true;
    
    /**
     * 是否脱敏显示
     * 默认为true，表示需要脱敏
     * 当用户没有查看权限时，会对数据进行脱敏处理
     */
    boolean mask() default true;
    
    /**
     * 查看未脱敏数据所需的权限
     * 如果为空字符串，则使用默认权限（VIEW_SENSITIVE_DATA）
     * 如果用户拥有此权限，则返回未脱敏的数据；否则返回脱敏后的数据
     * 
     * 常用权限：
     * - VIEW_SENSITIVE_DATA: 查看所有敏感数据
     * - VIEW_PHONE: 查看手机号
     * - VIEW_ID_CARD: 查看身份证号
     * - VIEW_BANK_CARD: 查看银行卡号
     * - VIEW_EMAIL: 查看邮箱
     * - VIEW_ADDRESS: 查看地址
     */
    String requiredPermission() default "";
}
