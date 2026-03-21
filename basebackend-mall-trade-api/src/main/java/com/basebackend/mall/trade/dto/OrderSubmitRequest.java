package com.basebackend.mall.trade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 订单提交请求
 */
public record OrderSubmitRequest(
        @NotEmpty(message = "下单商品不能为空")
        List<@Valid OrderItem> items) {

    /**
     * 下单明细
     */
    public record OrderItem(
            @NotNull(message = "SKU ID不能为空")
            Long skuId,

            @NotNull(message = "购买数量不能为空")
            @Positive(message = "购买数量必须大于0")
            Integer quantity) {
    }
}
