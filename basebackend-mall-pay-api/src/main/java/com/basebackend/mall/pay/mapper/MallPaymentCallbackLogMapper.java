package com.basebackend.mall.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.mall.pay.entity.MallPaymentCallbackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付回调日志 Mapper
 */
@Mapper
public interface MallPaymentCallbackLogMapper extends BaseMapper<MallPaymentCallbackLog> {
}
