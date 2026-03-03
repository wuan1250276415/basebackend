package com.basebackend.mall.product.event;

/**
 * 订单商品快照
 */
public record OrderItemSnapshot(
        Long skuId,
        Integer quantity) {
}
