package com.basebackend.mall.trade.event;

import com.basebackend.mall.trade.enums.MallOrderPayStatus;
import com.basebackend.mall.trade.enums.MallOrderStatus;

import java.util.List;

/**
 * 订单超时关闭消息体
 */
public record OrderTimeoutClosedMessage(
        Long orderId,
        String orderNo,
        String reason,
        MallOrderStatus orderStatus,
        MallOrderPayStatus orderPayStatus,
        List<OrderItemSnapshot> items) {
}
