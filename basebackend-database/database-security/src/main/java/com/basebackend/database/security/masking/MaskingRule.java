package com.basebackend.database.security.masking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏规则
 * 定义数据脱敏的具体规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingRule {
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 规则模式
     * 格式说明：
     * - # 表示保留原字符
     * - * 表示用*替换
     * - 其他字符表示直接使用该字符（如分隔符）
     * 
     * 例如：
     * - "###****####" 表示保留前3位和后4位，中间用*代替
     * - "***-****-####" 表示保留后4位，其他用*代替，并添加分隔符
     */
    private String pattern;
    
    /**
     * 保留前缀长度
     */
    private int prefixLength;
    
    /**
     * 保留后缀长度
     */
    private int suffixLength;
    
    /**
     * 脱敏字符
     */
    private String maskChar;
    
    /**
     * 是否启用
     */
    private boolean enabled;
    
    /**
     * 创建默认的手机号脱敏规则
     */
    public static MaskingRule phoneRule() {
        return MaskingRule.builder()
                .name("phone")
                .pattern("###****####")
                .prefixLength(3)
                .suffixLength(4)
                .maskChar("*")
                .enabled(true)
                .build();
    }
    
    /**
     * 创建默认的身份证号脱敏规则
     */
    public static MaskingRule idCardRule() {
        return MaskingRule.builder()
                .name("idCard")
                .pattern("######********####")
                .prefixLength(6)
                .suffixLength(4)
                .maskChar("*")
                .enabled(true)
                .build();
    }
    
    /**
     * 创建默认的银行卡号脱敏规则
     */
    public static MaskingRule bankCardRule() {
        return MaskingRule.builder()
                .name("bankCard")
                .pattern("#### **** **** ####")
                .prefixLength(4)
                .suffixLength(4)
                .maskChar("*")
                .enabled(true)
                .build();
    }
}
