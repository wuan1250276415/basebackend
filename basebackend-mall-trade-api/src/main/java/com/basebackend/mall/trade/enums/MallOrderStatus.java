package com.basebackend.mall.trade.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum MallOrderStatus {

    /**
     * 已创建
     */
    CREATED("CREATED", "已创建"),

    /**
     * 已支付
     */
    PAID("PAID", "已支付"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消"),

    /**
     * 超时关闭
     */
    TIMEOUT_CLOSED("TIMEOUT_CLOSED", "超时关闭");

    private final String code;
    private final String description;

    MallOrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码反查枚举
     *
     * @param code 状态码
     * @return 状态枚举
     */
    public static MallOrderStatus fromCode(String code) {
        for (MallOrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
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

