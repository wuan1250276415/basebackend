package com.basebackend.mall.pay.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建消息体
 */
public record OrderCreatedMessage(
        Long orderId,
        String orderNo,
        Long userId,
        BigDecimal payAmount,
        MallOrderStatus orderStatus,
        MallOrderPayStatus orderPayStatus,
        List<OrderItem> items) {

    /**
     * 订单商品项
     */
    public record OrderItem(
            Long skuId,
            Integer quantity) {
    }
}
