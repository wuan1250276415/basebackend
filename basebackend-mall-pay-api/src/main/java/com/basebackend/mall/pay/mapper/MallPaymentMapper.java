package com.basebackend.mall.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.pay.entity.MallPayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付单 Mapper
 */
@Mapper
public interface MallPaymentMapper extends BaseMapper<MallPayment> {
}
