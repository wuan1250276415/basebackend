package com.basebackend.mall.trade.event;

/**
 * 订单商品快照
 */
public record OrderItemSnapshot(
        Long skuId,
        Integer quantity) {
}
