package com.basebackend.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城SPU实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_spu")
public class MallSpu extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("spu_name")
    private String spuName;

    @TableField("brand_name")
    private String brandName;

    @TableField("sale_status")
    private Integer saleStatus;

    @TableField("description")
    private String description;
}
