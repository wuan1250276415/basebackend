package com.basebackend.chat.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 聊天搜索配置 — 仅当搜索服务启用时激活
 * <p>
 * 开启 {@link EnableAsync} 以支持消息异步索引。
 */
@Configuration
@ConditionalOnProperty(prefix = "basebackend.search", name = "enabled", havingValue = "true")
@EnableAsync
public class ChatSearchConfig {
}
