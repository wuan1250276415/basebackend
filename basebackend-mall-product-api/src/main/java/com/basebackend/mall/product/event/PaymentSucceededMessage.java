package com.basebackend.mall.product.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * 支付成功消息体（供库存扣减消费）
 */
public record PaymentSucceededMessage(
        String payNo,
        Long orderId,
        String orderNo,
        BigDecimal payAmount,
        MallPaymentStatus paymentStatus,
        List<PaidItem> items) {

    /**
     * 已支付商品明细
     */
    public record PaidItem(
            Long skuId,
            Integer quantity) {
    }
}
