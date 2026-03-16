package com.basebackend.mall.pay.event;

import lombok.Getter;

/**
 * 订单状态枚举（交易事件）
 */
@Getter
public enum MallOrderStatus {

    /**
     * 已创建
     */
    CREATED("CREATED"),

    /**
     * 已支付
     */
    PAID("PAID"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED"),

    /**
     * 超时关闭
     */
    TIMEOUT_CLOSED("TIMEOUT_CLOSED");

    private final String code;

    MallOrderStatus(String code) {
        this.code = code;
    }
}

