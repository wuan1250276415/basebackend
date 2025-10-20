package com.basebackend.messaging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 消息模块配置
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessagingConfig {
}
