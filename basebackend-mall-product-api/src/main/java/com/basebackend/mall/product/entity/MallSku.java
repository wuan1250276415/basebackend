package com.basebackend.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商城SKU实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_sku")
public class MallSku extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("spu_id")
    private Long spuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("market_price")
    private BigDecimal marketPrice;

    @TableField("stock_quantity")
    private Integer stockQuantity;

    @TableField("lock_quantity")
    private Integer lockQuantity;

    @TableField("sale_status")
    private Integer saleStatus;
}
