package com.basebackend.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城购物车实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_cart")
public class MallCart extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("checked_all")
    private Integer checkedAll;
}
