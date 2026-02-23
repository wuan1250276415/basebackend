package com.basebackend.messaging.config;

import com.basebackend.messaging.consumer.DeadLetterConsumer;
import com.basebackend.messaging.encryption.AesGcmMessageEncryptor;
import com.basebackend.messaging.encryption.MessageEncryptor;
import com.basebackend.messaging.event.EventPublisher;
import com.basebackend.messaging.idempotency.IdempotencyService;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import com.basebackend.messaging.mapper.MessageLogMapper;
import com.basebackend.messaging.mapper.WebhookEndpointMapper;
import com.basebackend.messaging.metrics.MessagingMetrics;
import com.basebackend.messaging.order.OrderedMessageConsumer;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.messaging.producer.RocketMQProducer;
import com.basebackend.messaging.tracing.MessageTracingService;
import com.basebackend.messaging.transaction.TransactionalMessageService;
import com.basebackend.messaging.webhook.WebhookInvoker;
import com.basebackend.messaging.webhook.WebhookSignatureService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@AutoConfiguration
@ConditionalOnProperty(prefix = "messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MessagingProperties.class)
@EnableScheduling
@MapperScan("com.basebackend.messaging.mapper")
@Import(MessagingExecutorConfig.class)
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate messagingRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "messaging.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotencyService idempotencyService(RedissonClient redissonClient, MessagingProperties properties) {
        return new IdempotencyService(redissonClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageLogMapper.class)
    @ConditionalOnProperty(prefix = "messaging.transaction", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TransactionalMessageService transactionalMessageService(MessageLogMapper messageLogMapper) {
        return new TransactionalMessageService(messageLogMapper);
    }

    @Bean
    @ConditionalOnMissingBean(MessageProducer.class)
    @ConditionalOnBean(RocketMQTemplate.class)
    public RocketMQProducer rocketMQProducer(RocketMQTemplate rocketMQTemplate,
                                              MessagingProperties messagingProperties,
                                              TransactionalMessageService transactionalMessageService,
                                              @Qualifier("messageSenderExecutor") ThreadPoolTaskExecutor senderExecutor) {
        return new RocketMQProducer(rocketMQTemplate, messagingProperties, transactionalMessageService, senderExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(WebhookEndpointMapper.class)
    public EventPublisher eventPublisher(WebhookEndpointMapper webhookEndpointMapper, WebhookInvoker webhookInvoker) {
        return new EventPublisher(webhookEndpointMapper, webhookInvoker);
    }

    @Bean
    @ConditionalOnMissingBean
    public WebhookSignatureService webhookSignatureService() {
        return new WebhookSignatureService();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebhookInvoker webhookInvoker(RestTemplate messagingRestTemplate,
                                          WebhookSignatureService signatureService,
                                          MessageProducer messageProducer) {
        return new WebhookInvoker(messagingRestTemplate, signatureService, messageProducer);
    }

    @Bean
    @ConditionalOnMissingBean
    public OrderedMessageConsumer orderedMessageConsumer() {
        return new OrderedMessageConsumer();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public MessageTracingService messageTracingService(StringRedisTemplate redisTemplate) {
        return new MessageTracingService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    public MessagingMetrics messagingMetrics(MeterRegistry registry) {
        return new MessagingMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean(MessageEncryptor.class)
    @ConditionalOnProperty(prefix = "messaging.encryption", name = "enabled", havingValue = "true")
    public AesGcmMessageEncryptor aesGcmMessageEncryptor(MessagingProperties properties) {
        return new AesGcmMessageEncryptor(properties);
    }
}
