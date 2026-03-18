package com.basebackend.messaging.config;

import com.basebackend.messaging.event.EventPublisher;
import com.basebackend.messaging.management.controller.*;
import com.basebackend.messaging.management.service.*;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import com.basebackend.messaging.mapper.MessageLogMapper;
import com.basebackend.messaging.mapper.WebhookEndpointMapper;
import com.basebackend.messaging.producer.MessageProducer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "messaging.management", name = "enabled", havingValue = "true")
public class MessagingManagementConfiguration {

    @Bean
    public WebhookManagementService webhookManagementService(WebhookEndpointMapper webhookEndpointMapper) {
        return new WebhookManagementService(webhookEndpointMapper);
    }

    @Bean
    public DeadLetterManagementService deadLetterManagementService(DeadLetterMapper deadLetterMapper,
                                                                   ObjectProvider<MessageProducer> messageProducerProvider) {
        return new DeadLetterManagementService(deadLetterMapper, messageProducerProvider);
    }

    @Bean
    public MessagingMonitorService messagingMonitorService(MessageLogMapper messageLogMapper,
                                                           DeadLetterMapper deadLetterMapper,
                                                           ListableBeanFactory beanFactory) {
        return new MessagingMonitorService(messageLogMapper, deadLetterMapper, beanFactory);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public WebhookLogStore webhookLogStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisWebhookLogStore(stringRedisTemplate);
    }

    @Bean
    public WebhookManagementController webhookManagementController(WebhookManagementService webhookManagementService) {
        return new WebhookManagementController(webhookManagementService);
    }

    @Bean
    public DeadLetterManagementController deadLetterManagementController(DeadLetterManagementService deadLetterManagementService) {
        return new DeadLetterManagementController(deadLetterManagementService);
    }

    @Bean
    public MessagingEventController messagingEventController(ObjectProvider<EventPublisher> eventPublisherProvider) {
        return new MessagingEventController(eventPublisherProvider);
    }

    @Bean
    public MessagingMonitorController messagingMonitorController(MessagingMonitorService messagingMonitorService) {
        return new MessagingMonitorController(messagingMonitorService);
    }

    @Bean
    public WebhookLogController webhookLogController(ObjectProvider<WebhookLogStore> webhookLogStoreProvider) {
        return new WebhookLogController(webhookLogStoreProvider);
    }
}
