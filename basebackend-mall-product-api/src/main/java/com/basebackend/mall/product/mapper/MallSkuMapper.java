package com.basebackend.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.product.entity.MallSku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商城SKU Mapper
 */
@Mapper
public interface MallSkuMapper extends BaseMapper<MallSku> {
}
