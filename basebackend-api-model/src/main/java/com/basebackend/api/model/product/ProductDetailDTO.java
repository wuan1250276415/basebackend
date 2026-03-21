package com.basebackend.api.model.product;

import java.math.BigDecimal;

/**
 * 商品详情共享 DTO
 */
public record ProductDetailDTO(
        Long skuId,
        Long spuId,
        String skuCode,
        String skuName,
        BigDecimal salePrice,
        Integer availableStock,
        Boolean onShelf) {
}
