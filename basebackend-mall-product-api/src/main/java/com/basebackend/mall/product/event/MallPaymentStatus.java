package com.basebackend.mall.product.event;

import lombok.Getter;

/**
 * 支付单状态枚举（支付事件）
 */
@Getter
public enum MallPaymentStatus {

    /**
     * 待支付
     */
    WAIT_PAY("WAIT_PAY"),

    /**
     * 支付成功
     */
    PAY_SUCCESS("PAY_SUCCESS"),

    /**
     * 支付失败
     */
    PAY_FAILED("PAY_FAILED"),

    /**
     * 已关闭
     */
    CLOSED("CLOSED");

    private final String code;

    MallPaymentStatus(String code) {
        this.code = code;
    }
}

