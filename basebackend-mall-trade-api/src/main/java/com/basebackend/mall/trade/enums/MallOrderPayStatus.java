package com.basebackend.mall.trade.enums;

import lombok.Getter;

/**
 * 订单支付状态枚举
 */
@Getter
public enum MallOrderPayStatus {

    /**
     * 未支付
     */
    UNPAID("UNPAID", "未支付"),

    /**
     * 已支付
     */
    PAID("PAID", "已支付"),

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

    MallOrderPayStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码反查枚举
     *
     * @param code 状态码
     * @return 支付状态枚举
     */
    public static MallOrderPayStatus fromCode(String code) {
        for (MallOrderPayStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单支付状态: " + code);
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

