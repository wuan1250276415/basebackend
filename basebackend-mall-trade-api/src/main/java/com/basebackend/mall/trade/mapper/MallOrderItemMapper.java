package com.basebackend.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.trade.entity.MallOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface MallOrderItemMapper extends BaseMapper<MallOrderItem> {
}
