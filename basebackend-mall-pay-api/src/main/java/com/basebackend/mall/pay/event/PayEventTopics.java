package com.basebackend.mall.pay.event;

/**
 * 支付域事件 Topic 约定
 */
public final class PayEventTopics {

    private PayEventTopics() {
    }

    /**
     * 支付单创建
     */
    public static final String PAYMENT_CREATED = "mall.pay.payment-created";

    /**
     * 支付成功
     */
    public static final String PAYMENT_SUCCEEDED = "mall.pay.payment-succeeded";

    /**
     * 支付失败
     */
    public static final String PAYMENT_FAILED = "mall.pay.payment-failed";

    /**
     * 退款单创建
     */
    public static final String REFUND_CREATED = "mall.pay.refund-created";

    /**
     * 退款成功
     */
    public static final String REFUND_SUCCEEDED = "mall.pay.refund-succeeded";
}
