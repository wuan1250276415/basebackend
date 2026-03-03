package com.basebackend.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商城订单明细实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order_item")
public class MallOrderItem extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("sku_id")
    private Long skuId;

    @TableField("sku_name")
    private String skuName;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("quantity")
    private Integer quantity;

    @TableField("line_amount")
    private BigDecimal lineAmount;
}
