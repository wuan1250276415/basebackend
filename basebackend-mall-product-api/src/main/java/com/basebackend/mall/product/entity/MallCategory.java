package com.basebackend.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城类目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_category")
public class MallCategory extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("category_name")
    private String categoryName;

    @TableField("parent_id")
    private Long parentId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;
}
