package com.basebackend.mall.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.pay.entity.MallRefund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款单 Mapper
 */
@Mapper
public interface MallRefundMapper extends BaseMapper<MallRefund> {
}
