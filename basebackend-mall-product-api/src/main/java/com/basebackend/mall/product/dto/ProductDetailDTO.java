package com.basebackend.mall.product.dto;

import java.math.BigDecimal;

/**
 * 商品详情DTO
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
