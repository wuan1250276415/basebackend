package com.basebackend.mall.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 创建支付单请求
 */
public record PaymentCreateRequest(
        @NotNull(message = "订单ID不能为空")
        Long orderId,

        @NotBlank(message = "订单号不能为空")
        String orderNo,

        @NotNull(message = "支付金额不能为空")
        @Positive(message = "支付金额必须大于0")
        BigDecimal payAmount,

        @NotBlank(message = "支付渠道不能为空")
        String payChannel) {
}
