package com.basebackend.mall.pay.dto;

import com.basebackend.mall.pay.enums.MallPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建支付单响应
 */
@Schema(description = "创建支付单响应")
public record PaymentCreateResponse(
        @Schema(description = "支付单号", example = "PAY202603031430003001")
        String payNo,

        @Schema(
                description = "支付单状态",
                example = "WAIT_PAY",
                allowableValues = {"WAIT_PAY", "PAY_SUCCESS", "PAY_FAILED", "CLOSED"}
        )
        MallPaymentStatus payStatus,

        @Schema(description = "支付链接（联调mock）", example = "/api/mall/payments/PAY202603031430003001/mock-pay")
        String payUrl) {
}
