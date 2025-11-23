package com.basebackend.logging.masking;

/**
 * 脱敏策略枚举
 *
 * 定义了多种脱敏方式，适用于不同的敏感信息场景。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public enum MaskingStrategy {

    /**
     * 掩码策略（MASK）
     * 全部替换为指定字符，如：138****8888
     * 适用于：需要保留格式的场景
     */
    MASK,

    /**
     * 部分保留策略（PARTIAL）
     * 保留前缀和后缀，中间部分脱敏
     * 适用于：手机号、身份证、银行卡等
     */
    PARTIAL,

    /**
     * 哈希策略（HASH）
     * 使用SHA-256哈希值替换原值
     * 适用于：需要不可逆脱敏的场景
     */
    HASH,

    /**
     * 移除策略（REMOVE）
     * 完全移除敏感字段，替换为空字符串
     * 适用于：极度敏感的信息
     */
    REMOVE,

    /**
     * 自定义策略（CUSTOM）
     * 使用自定义字符串替换
     * 适用于：特殊需求的场景
     */
    CUSTOM
}
