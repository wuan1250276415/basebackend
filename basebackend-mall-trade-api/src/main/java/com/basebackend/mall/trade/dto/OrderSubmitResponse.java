package com.basebackend.mall.trade.dto;

import com.basebackend.mall.trade.enums.MallOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 订单提交响应
 */
@Schema(description = "订单提交响应")
public record OrderSubmitResponse(
        @Schema(description = "订单号", example = "TRD2026030314300010001")
        String orderNo,

        @Schema(
                description = "订单状态",
                example = "CREATED",
                allowableValues = {"CREATED", "PAID", "CANCELLED", "TIMEOUT_CLOSED"}
        )
        MallOrderStatus orderStatus,

        @Schema(description = "实付金额", example = "198.00")
        BigDecimal payAmount) {
}
