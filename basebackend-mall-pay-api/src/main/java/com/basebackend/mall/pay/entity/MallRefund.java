package com.basebackend.mall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_refund")
public class MallRefund extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("refund_no")
    private String refundNo;

    @TableField("pay_no")
    private String payNo;

    @TableField("order_no")
    private String orderNo;

    @TableField("refund_status")
    private String refundStatus;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("reason")
    private String reason;

    @TableField("third_party_refund_no")
    private String thirdPartyRefundNo;

    @TableField("refund_time")
    private LocalDateTime refundTime;
}
