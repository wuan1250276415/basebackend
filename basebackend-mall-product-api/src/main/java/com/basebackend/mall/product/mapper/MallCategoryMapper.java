package com.basebackend.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.product.entity.MallCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商城类目 Mapper
 */
@Mapper
public interface MallCategoryMapper extends BaseMapper<MallCategory> {
}
