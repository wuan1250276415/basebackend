package com.basebackend.admin.mapper.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.messaging.SysWebhookLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Webhook调用日志Mapper
 */
@Mapper
public interface SysWebhookLogMapper extends BaseMapper<SysWebhookLog> {
}
