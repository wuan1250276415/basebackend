package com.basebackend.messaging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * RocketMQ 配置
 *
 * <p>RocketMQTemplate 由 rocketmq-spring-boot-starter 自动配置，
 * 此处仅提供自定义的 MessageConverter。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "messaging.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMQConfig {

    /**
     * 配置消息转换器（Jackson）
     */
    @Bean
    public MessageConverter messageConverter() {
        return new MappingJackson2MessageConverter();
    }
}
