package com.basebackend.admin.mapper.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.messaging.SysWebhookConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Webhook配置Mapper
 */
@Mapper
public interface SysWebhookConfigMapper extends BaseMapper<SysWebhookConfig> {
}
