package com.basebackend.mall.pay.event;

/**
 * 订单商品快照
 */
public record OrderItemSnapshot(
        Long skuId,
        Integer quantity) {
}
