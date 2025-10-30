package com.basebackend.messaging.config;

import com.basebackend.messaging.constants.RocketMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * RocketMQ 配置
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMQConfig {

    private final MessagingProperties messagingProperties;

    /**
     * 配置消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new MappingJackson2MessageConverter();
    }

    /**
     * 配置 RocketMQTemplate（可选，用于自定义配置）
     * RocketMQTemplate 会由 spring-boot-starter 自动配置
     */
    @Bean
    public RocketMQTemplate rocketMQTemplate(org.apache.rocketmq.spring.autoconfigure.RocketMQProperties rocketMQProperties) {
        log.info("初始化 RocketMQTemplate, NameServer: {}", rocketMQProperties.getNameServer());
        log.info("生产者组: {}", rocketMQProperties.getProducer().getGroup());
        log.info("默认Topic: {}", messagingProperties.getRocketmq().getDefaultTopic());

        return new RocketMQTemplate();
    }
}
