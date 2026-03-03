package com.basebackend.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.trade.entity.MallCart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper
 */
@Mapper
public interface MallCartMapper extends BaseMapper<MallCart> {
}
