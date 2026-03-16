package com.basebackend.mall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_payment")
public class MallPayment extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("pay_no")
    private String payNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("pay_status")
    private String payStatus;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("third_party_trade_no")
    private String thirdPartyTradeNo;

    @TableField("order_items_json")
    private String orderItemsJson;

    @TableField("paid_time")
    private LocalDateTime paidTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;
}
