package com.basebackend.mall.pay.enums;

import lombok.Getter;

/**
 * 支付单状态枚举
 */
@Getter
public enum MallPaymentStatus {

    /**
     * 待支付
     */
    WAIT_PAY("WAIT_PAY", "待支付"),

    /**
     * 支付成功
     */
    PAY_SUCCESS("PAY_SUCCESS", "支付成功"),

    /**
     * 支付失败
     */
    PAY_FAILED("PAY_FAILED", "支付失败"),

    /**
     * 已关闭
     */
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String description;

    MallPaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码反查枚举
     *
     * @param code 状态码
     * @return 支付状态枚举
     */
    public static MallPaymentStatus fromCode(String code) {
        for (MallPaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知支付单状态: " + code);
    }

    /**
     * 判断状态码是否匹配
     *
     * @param code 状态码
     * @return 是否匹配
     */
    public boolean matches(String code) {
        return this.code.equals(code);
    }
}

