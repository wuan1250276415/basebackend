package com.basebackend.mall.pay.service;

import com.basebackend.mall.pay.dto.PaymentCreateRequest;
import com.basebackend.mall.pay.dto.PaymentCreateResponse;
import com.basebackend.mall.pay.event.OrderCreatedMessage;
import com.basebackend.mall.pay.event.OrderTimeoutClosedMessage;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 健康检查信息
     *
     * @return 服务状态
     */
    String ping();

    /**
     * 创建支付单
     *
     * @param request 创建支付单请求
     * @return 支付单信息
     */
    PaymentCreateResponse createPayment(PaymentCreateRequest request);

    /**
     * 处理下单事件并创建支付记录
     *
     * @param message 下单事件消息
     */
    void handleOrderCreated(OrderCreatedMessage message);

    /**
     * 模拟支付成功
     *
     * @param orderNo 订单号
     */
    void mockPaySuccess(String orderNo);

    /**
     * 模拟支付失败
     *
     * @param orderNo 订单号
     * @param reason  失败原因
     */
    void mockPayFail(String orderNo, String reason);

    /**
     * 处理订单超时关闭，联动关闭支付单
     *
     * @param message 超时关单消息
     */
    void handleOrderTimeoutClosed(OrderTimeoutClosedMessage message);
}
