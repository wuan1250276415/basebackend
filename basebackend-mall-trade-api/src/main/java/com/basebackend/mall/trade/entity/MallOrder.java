package com.basebackend.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城订单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class MallOrder extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("order_status")
    private String orderStatus;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("pay_status")
    private String payStatus;

    @TableField("remark")
    private String remark;

    @TableField("submit_time")
    private LocalDateTime submitTime;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("close_time")
    private LocalDateTime closeTime;
}
