package com.basebackend.mall.trade.service;

import com.basebackend.mall.trade.dto.OrderSubmitRequest;
import com.basebackend.mall.trade.dto.OrderSubmitResponse;
import com.basebackend.mall.trade.event.PaymentFailedMessage;
import com.basebackend.mall.trade.event.PaymentSucceededMessage;

/**
 * 交易服务接口
 */
public interface TradeService {

    /**
     * 健康检查信息
     *
     * @return 服务状态
     */
    String ping();

    /**
     * 提交订单
     *
     * @param request 下单请求
     * @return 下单结果
     */
    OrderSubmitResponse submitOrder(OrderSubmitRequest request);

    /**
     * 处理支付成功回调事件
     *
     * @param message 支付成功消息
     */
    void markOrderPaid(PaymentSucceededMessage message);

    /**
     * 处理支付失败回调事件
     *
     * @param message 支付失败消息
     */
    void markOrderPaymentFailed(PaymentFailedMessage message);

    /**
     * 关闭超时未支付订单
     *
     * @param orderNo 订单号
     */
    void closeTimeoutOrder(String orderNo);
}
