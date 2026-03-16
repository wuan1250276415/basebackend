package com.basebackend.ticket.config;

import org.springframework.context.annotation.Configuration;

/**
 * 工单数据脱敏配置
 * <p>masking 模块自动注册了默认的 PHONE/EMAIL 脱敏策略，
 * 此配置类预留用于注册工单专属的脱敏策略扩展</p>
 */
@Configuration
public class TicketMaskingConfig {
    // 默认的 MaskType.PHONE / EMAIL 策略已由 basebackend-common-masking 自动注册
    // 如需自定义工单专属脱敏策略，可在此注入 MaskingStrategyRegistry 并 register
}
