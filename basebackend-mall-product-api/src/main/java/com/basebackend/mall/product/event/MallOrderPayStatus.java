package com.basebackend.mall.product.event;

import lombok.Getter;

/**
 * 订单支付状态枚举（交易事件）
 */
@Getter
public enum MallOrderPayStatus {

    /**
     * 未支付
     */
    UNPAID("UNPAID"),

    /**
     * 已支付
     */
    PAID("PAID"),

    /**
     * 支付失败
     */
    PAY_FAILED("PAY_FAILED"),

    /**
     * 已关闭
     */
    CLOSED("CLOSED");

    private final String code;

    MallOrderPayStatus(String code) {
        this.code = code;
    }
}

