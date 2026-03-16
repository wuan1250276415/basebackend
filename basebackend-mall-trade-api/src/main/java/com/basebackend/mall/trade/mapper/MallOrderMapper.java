package com.basebackend.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.trade.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {
}
