package com.basebackend.mall.pay.event;

import com.basebackend.mall.pay.enums.MallPaymentStatus;

/**
 * 支付失败消息体
 */
public record PaymentFailedMessage(
        String payNo,
        Long orderId,
        String orderNo,
        MallPaymentStatus paymentStatus,
        String reason) {
}
