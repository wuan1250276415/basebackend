package com.basebackend.mall.trade.event;

/**
 * 交易域事件 Topic 约定
 */
public final class TradeEventTopics {

    private TradeEventTopics() {
    }

    /**
     * 订单创建
     */
    public static final String ORDER_CREATED = "mall.trade.order-created";

    /**
     * 订单取消
     */
    public static final String ORDER_CANCELLED = "mall.trade.order-cancelled";

    /**
     * 订单支付成功
     */
    public static final String ORDER_PAID = "mall.trade.order-paid";

    /**
     * 订单超时关闭
     */
    public static final String ORDER_TIMEOUT_CLOSED = "mall.trade.order-timeout-closed";
}
