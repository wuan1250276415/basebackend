package com.basebackend.mall.product.event;

import java.util.List;

/**
 * 订单取消消息体
 */
public record OrderCancelledMessage(
        Long orderId,
        String orderNo,
        String reason,
        MallOrderStatus orderStatus,
        MallOrderPayStatus orderPayStatus,
        List<OrderItemSnapshot> items) {
}
