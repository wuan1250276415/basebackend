package com.basebackend.mall.trade.event;

import com.basebackend.mall.trade.enums.MallOrderPayStatus;
import com.basebackend.mall.trade.enums.MallOrderStatus;

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
