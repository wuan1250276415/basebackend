package com.basebackend.observability.logging.masking;

/**
 * 脱敏策略接口
 * <p>
 * 定义敏感信息脱敏的策略接口，支持自定义脱敏规则。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface MaskingStrategy {

    /**
     * 对敏感信息进行脱敏
     *
     * @param input 原始输入
     * @return 脱敏后的内容
     */
    String mask(String input);

    /**
     * 完全隐藏策略
     */
    MaskingStrategy FULL_MASK = input -> "******";

    /**
     * 保留首尾策略（保留首1位和末1位）
     */
    MaskingStrategy KEEP_ENDS = input -> {
        if (input == null || input.length() <= 2) {
            return "***";
        }
        return input.charAt(0) + "***" + input.charAt(input.length() - 1);
    };

    /**
     * 手机号脱敏策略（保留前3后4）
     */
    MaskingStrategy PHONE = input -> {
        if (input == null || input.length() < 7) {
            return "***";
        }
        return input.substring(0, 3) + "****" + input.substring(input.length() - 4);
    };

    /**
     * 身份证号脱敏策略（保留前6后4）
     */
    MaskingStrategy ID_CARD = input -> {
        if (input == null || input.length() < 10) {
            return "***";
        }
        return input.substring(0, 6) + "********" + input.substring(input.length() - 4);
    };

    /**
     * 邮箱脱敏策略（保留@前3位和@后域名）
     */
    MaskingStrategy EMAIL = input -> {
        if (input == null || !input.contains("@")) {
            return "***@***.***";
        }
        int atIndex = input.indexOf("@");
        String local = input.substring(0, atIndex);
        String domain = input.substring(atIndex);

        if (local.length() <= 3) {
            return "***" + domain;
        }
        return local.substring(0, 3) + "***" + domain;
    };

    /**
     * 银行卡脱敏策略（保留前4后4）
     */
    MaskingStrategy BANK_CARD = input -> {
        if (input == null || input.length() < 8) {
            return "***";
        }
        return input.substring(0, 4) + "********" + input.substring(input.length() - 4);
    };
}
