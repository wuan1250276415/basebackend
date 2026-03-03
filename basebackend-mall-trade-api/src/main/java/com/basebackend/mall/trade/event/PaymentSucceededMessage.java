package com.basebackend.mall.trade.event;

import com.basebackend.mall.trade.enums.MallPaymentStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 支付成功消息体
 */
public record PaymentSucceededMessage(
        String payNo,
        Long orderId,
        String orderNo,
        BigDecimal payAmount,
        MallPaymentStatus paymentStatus,
        List<PaidItem> items) {

    /**
     * 已支付商品项
     */
    public record PaidItem(
            Long skuId,
            Integer quantity) {
    }
}
